/**
 * StreamBD / Toffee Reverse Proxy & HLS Rewriter (Bundled)
 * Deployed to Cloudflare Workers
 */

/**
 * AES-256-GCM Stream Token Decryption & Encryption
 * Matches Android client implementation in vf4.java
 */

async function decryptStreamToken(token, secret) {
  if (!token || !secret) return null;

  try {
    // 1. Base64 URL decode
    let b64 = token.replace(/-/g, "+").replace(/_/g, "/");
    while (b64.length % 4 !== 0) {
      b64 += "=";
    }

    const binary = atob(b64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }

    if (bytes.length < 28) {
      // Must be at least 12 bytes IV + 16 bytes Tag
      return null;
    }

    const iv = bytes.slice(0, 12);
    const ciphertextWithTag = bytes.slice(12);

    // 2. Derive AES key: SHA-256(secret)
    const enc = new TextEncoder();
    const keyHash = await crypto.subtle.digest("SHA-256", enc.encode(secret));
    const cryptoKey = await crypto.subtle.importKey(
      "raw",
      keyHash,
      { name: "AES-GCM" },
      false,
      ["decrypt"]
    );

    // 3. Decrypt ciphertext
    const decryptedBuffer = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: iv, tagLength: 128 },
      cryptoKey,
      ciphertextWithTag
    );

    const decryptedStr = new TextDecoder().decode(decryptedBuffer);
    const data = JSON.parse(decryptedStr);

    // 4. Validate expiration (if specified)
    const now = Math.floor(Date.now() / 1000);
    if (data.exp && data.exp < now) {
      console.warn("Stream token has expired:", data.exp, "vs now:", now);
      return null;
    }

    return data; // { u: "https://...", r: "https://...", exp: 12345678 }
  } catch (err) {
    console.error("Failed to decrypt stream token:", err.message);
    return null;
  }
}

async function encryptStreamToken(targetUrl, referer, secret, ttlSeconds = 21600) {
  try {
    const enc = new TextEncoder();
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const exp = Math.floor(Date.now() / 1000) + ttlSeconds;

    const payload = JSON.stringify({
      u: targetUrl,
      r: referer || "",
      exp: exp
    });

    const keyHash = await crypto.subtle.digest("SHA-256", enc.encode(secret));
    const cryptoKey = await crypto.subtle.importKey(
      "raw",
      keyHash,
      { name: "AES-GCM" },
      false,
      ["encrypt"]
    );

    const encryptedBuffer = await crypto.subtle.encrypt(
      { name: "AES-GCM", iv: iv, tagLength: 128 },
      cryptoKey,
      enc.encode(payload)
    );

    const out = new Uint8Array(12 + encryptedBuffer.byteLength);
    out.set(iv, 0);
    out.set(new Uint8Array(encryptedBuffer), 12);

    let binary = "";
    for (let i = 0; i < out.length; i++) {
      binary += String.fromCharCode(out[i]);
    }

    return btoa(binary)
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  } catch (err) {
    console.error("Token encryption failed:", err);
    return null;
  }
}

/**
 * HLS (.m3u8) Playlist Parser & URL Rewriter
 * Rewrites media segments and sub-manifests to route through the proxy worker.
 */


async function rewriteHlsManifest(manifestText, targetBaseUrl, proxyOrigin, secret, referer) {
  const lines = manifestText.split(/\r?\n/);
  const rewrittenLines = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();

    // Preserve blank lines
    if (!line) {
      rewrittenLines.push("");
      continue;
    }

    // Handle URI inside tags like #EXT-X-KEY:METHOD=...,URI="..." or #EXT-X-MAP:URI="..."
    if (line.startsWith("#")) {
      if (line.includes('URI="')) {
        const rewrittenTag = await replaceUriInTag(line, targetBaseUrl, proxyOrigin, secret, referer);
        rewrittenLines.push(rewrittenTag);
      } else {
        rewrittenLines.push(line);
      }
      continue;
    }

    // Non-tag lines are stream or segment URLs
    try {
      const absoluteUrl = new URL(line, targetBaseUrl).href;
      const token = await encryptStreamToken(absoluteUrl, referer, secret, 21600);
      const proxiedUrl = `${proxyOrigin}/hls?t=${encodeURIComponent(token)}`;
      rewrittenLines.push(proxiedUrl);
    } catch (e) {
      // If URL parsing fails, keep original line
      rewrittenLines.push(line);
    }
  }

  return rewrittenLines.join("\n");
}

async function replaceUriInTag(tagLine, targetBaseUrl, proxyOrigin, secret, referer) {
  const uriMatch = tagLine.match(/URI="([^"]+)"/);
  if (!uriMatch) return tagLine;

  const rawUri = uriMatch[1];
  try {
    const absoluteUri = new URL(rawUri, targetBaseUrl).href;
    const token = await encryptStreamToken(absoluteUri, referer, secret, 21600);
    const proxiedUri = `${proxyOrigin}/hls?t=${encodeURIComponent(token)}`;
    return tagLine.replace(`URI="${rawUri}"`, `URI="${proxiedUri}"`);
  } catch (e) {
    return tagLine;
  }
}

/**
 * StreamBD HLS & Media Proxy Cloudflare Worker
 * Handles encrypted stream tokens, HLS manifest rewriting, and Toffee cookie generation.
 */


const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
  "Access-Control-Allow-Headers": "Range, Content-Type, Authorization, User-Agent, Referer, Origin",
  "Access-Control-Expose-Headers": "Content-Length, Content-Range, Accept-Ranges",
  "Access-Control-Max-Age": "86400"
};

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...CORS_HEADERS
    }
  });
}

export default {
  async fetch(request, env, ctx) {
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS_HEADERS });
    }

    const url = new URL(request.url);
    const path = url.pathname;
    const secret = env.STREAM_SECRET || "streambd_K9mR2pL8nT5qY3wJ7cS4uE6oA0bF";
    const defaultUa = env.DEFAULT_USER_AGENT || "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0 Safari/537.36";

    try {
      // 1. Toffee Live Cookie / Session Endpoint
      if (url.searchParams.get("get_cookie") === "1" || path === "/get_cookie") {
        // Return fresh session cookie parameters
        const expTime = Math.floor(Date.now() / 1000) + 86400;
        const cookieVal = `Edge-Cache-Cookie=URLPrefix=aHR0cHM6Ly9wcm9kLWNkbjAxLWxpdmUudG9mZmVlbGl2ZS5jb20~Expires=${expTime}`;

        return jsonResponse({
          ok: true,
          cookie: cookieVal,
          user_agent: env.TOFFEE_USER_AGENT || "Toffee (Linux; Android 13)",
          referer: env.TOFFEE_REFERER || "https://toffeelive.com/",
          expires_at: expTime
        });
      }

      // 2. Encrypt Token Helper (/encrypt?url=...&ref=...)
      if (path === "/encrypt") {
        const targetUrl = url.searchParams.get("url");
        const ref = url.searchParams.get("ref") || "";
        if (!targetUrl) return jsonResponse({ error: "Missing 'url' query parameter" }, 400);

        const token = await encryptStreamToken(targetUrl, ref, secret);
        const proxiedUrl = `${url.origin}/hls?t=${encodeURIComponent(token)}`;
        return jsonResponse({
          ok: true,
          token,
          proxied_url: proxiedUrl
        });
      }

      // 3. HLS Stream Proxy Handler (/hls or /stream or /proxy)
      let targetStreamUrl = null;
      let targetReferer = null;

      const tokenParam = url.searchParams.get("t") || url.searchParams.get("token");
      if (tokenParam) {
        const decrypted = await decryptStreamToken(tokenParam, secret);
        if (!decrypted || !decrypted.u) {
          return new Response("Invalid or expired stream token", { status: 403, headers: CORS_HEADERS });
        }
        targetStreamUrl = decrypted.u;
        targetReferer = decrypted.r || "";
      } else if (url.searchParams.has("url")) {
        // Plaintext fallback for testing: /hls?url=...&ref=...
        targetStreamUrl = url.searchParams.get("url");
        targetReferer = url.searchParams.get("ref") || "";
      }

      if (targetStreamUrl) {
        return await handleStreamProxy(request, targetStreamUrl, targetReferer, url.origin, secret, defaultUa);
      }

      // 4. Root Status & Documentation
      return jsonResponse({
        service: "StreamBD Proxy Worker",
        status: "active",
        version: "1.2",
        usage: {
          cookie_endpoint: `${url.origin}/?get_cookie=1`,
          token_proxy: `${url.origin}/hls?t=<encrypted_token>`,
          plain_proxy: `${url.origin}/hls?url=<encoded_target_url>`
        }
      });
    } catch (err) {
      console.error("Stream Proxy error:", err);
      return new Response("Stream Proxy Error: " + err.message, {
        status: 500,
        headers: CORS_HEADERS
      });
    }
  }
};

async function handleStreamProxy(clientRequest, targetUrl, referer, proxyOrigin, secret, defaultUa) {
  // Build upstream request headers
  const upstreamHeaders = new Headers();
  upstreamHeaders.set("User-Agent", defaultUa);
  if (referer && referer.trim()) {
    upstreamHeaders.set("Referer", referer.trim());
    try {
      upstreamHeaders.set("Origin", new URL(referer.trim()).origin);
    } catch (e) {}
  }

  // Forward Range headers for video chunk seeking
  const clientRange = clientRequest.headers.get("Range");
  if (clientRange) {
    upstreamHeaders.set("Range", clientRange);
  }

  // Fetch upstream media
  const upstreamResponse = await fetch(targetUrl, {
    method: clientRequest.method === "HEAD" ? "HEAD" : "GET",
    headers: upstreamHeaders,
    redirect: "follow"
  });

  const contentType = (upstreamResponse.headers.get("Content-Type") || "").toLowerCase();
  const isHls = contentType.includes("mpegurl") ||
                contentType.includes("application/x-mpegurl") ||
                contentType.includes("application/vnd.apple.mpegurl") ||
                targetUrl.includes(".m3u8");

  // If HLS playlist, rewrite URLs to route through proxy
  if (isHls && clientRequest.method !== "HEAD") {
    const rawManifest = await upstreamResponse.text();
    const finalUrl = upstreamResponse.url || targetUrl;
    const rewritten = await rewriteHlsManifest(rawManifest, finalUrl, proxyOrigin, secret, referer);

    return new Response(rewritten, {
      status: upstreamResponse.status,
      headers: {
        "Content-Type": "application/vnd.apple.mpegurl",
        "Cache-Control": "no-cache, no-store, must-revalidate",
        ...CORS_HEADERS
      }
    });
  }

  // For video segments (.ts, .aac, .mp4, chunks), stream response body directly
  const responseHeaders = new Headers();
  for (const [k, v] of Object.entries(CORS_HEADERS)) {
    responseHeaders.set(k, v);
  }

  // Pass necessary media headers
  const forwardHeaders = ["Content-Type", "Content-Length", "Content-Range", "Accept-Ranges", "Cache-Control"];
  for (const h of forwardHeaders) {
    const val = upstreamResponse.headers.get(h);
    if (val) responseHeaders.set(h, val);
  }

  return new Response(upstreamResponse.body, {
    status: upstreamResponse.status,
    statusText: upstreamResponse.statusText,
    headers: responseHeaders
  });
}
