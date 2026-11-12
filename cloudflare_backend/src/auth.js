/**
 * Authentication and HMAC-SHA256 Verification using Native Web Crypto API
 */

export async function verifyHmacSignature(timestamp, signature, secret) {
  if (!timestamp || !signature || !secret) {
    return false;
  }

  try {
    const enc = new TextEncoder();
    const key = await crypto.subtle.importKey(
      "raw",
      enc.encode(secret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );

    const sigBuffer = await crypto.subtle.sign(
      "HMAC",
      key,
      enc.encode(timestamp)
    );

    const computedSig = Array.from(new Uint8Array(sigBuffer))
      .map(b => b.toString(16).padStart(2, "0"))
      .join("");

    return computedSig.toLowerCase() === signature.toLowerCase().trim();
  } catch (err) {
    console.error("HMAC verification error:", err);
    return false;
  }
}

/**
 * Generate a signed Bearer Token valid for 24 hours.
 */
export async function generateAuthToken(secret) {
  const header = { alg: "HS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: "hamyra-client",
    iat: now,
    exp: now + 86400 // 24 hours
  };

  const enc = new TextEncoder();
  const base64UrlEncode = (obj) => {
    const str = JSON.stringify(obj);
    const bytes = enc.encode(str);
    let binary = "";
    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary)
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  };

  const unsignedToken = `${base64UrlEncode(header)}.${base64UrlEncode(payload)}`;

  const key = await crypto.subtle.importKey(
    "raw",
    enc.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const sigBuffer = await crypto.subtle.sign(
    "HMAC",
    key,
    enc.encode(unsignedToken)
  );

  const signature = Array.from(new Uint8Array(sigBuffer))
    .map(b => String.fromCharCode(b))
    .join("");

  const base64UrlSig = btoa(signature)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");

  return `${unsignedToken}.${base64UrlSig}`;
}

export async function verifyAuthToken(token, secret) {
  if (!token || typeof token !== "string") return false;
  const parts = token.split(".");
  if (parts.length !== 3) return false;

  const [headerB64, payloadB64, sigB64] = parts;
  try {
    const enc = new TextEncoder();
    const unsignedToken = `${headerB64}.${payloadB64}`;

    const key = await crypto.subtle.importKey(
      "raw",
      enc.encode(secret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );

    const sigBuffer = await crypto.subtle.sign("HMAC", key, enc.encode(unsignedToken));
    const computedSigB64 = btoa(Array.from(new Uint8Array(sigBuffer)).map(b => String.fromCharCode(b)).join(""))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");

    if (computedSigB64 !== sigB64) return false;

    // Check expiration
    const payloadJson = atob(payloadB64.replace(/-/g, "+").replace(/_/g, "/"));
    const payload = JSON.parse(payloadJson);
    const now = Math.floor(Date.now() / 1000);
    return payload.exp > now;
  } catch (err) {
    return false;
  }
}
