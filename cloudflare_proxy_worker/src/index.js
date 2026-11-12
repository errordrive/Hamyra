/**
 * StreamBD HLS & Media Proxy Cloudflare Worker
 * Handles encrypted stream tokens, HLS manifest rewriting, and Toffee cookie generation.
 */

import { decryptStreamToken, encryptStreamToken } from "./crypto.js";
import { rewriteHlsManifest } from "./hls.js";

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
