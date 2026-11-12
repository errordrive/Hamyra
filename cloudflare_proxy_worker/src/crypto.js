/**
 * AES-256-GCM Stream Token Decryption & Encryption
 * Matches Android client implementation in vf4.java
 */

export async function decryptStreamToken(token, secret) {
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

export async function encryptStreamToken(targetUrl, referer, secret, ttlSeconds = 21600) {
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
