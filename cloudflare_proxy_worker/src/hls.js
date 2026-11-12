/**
 * HLS (.m3u8) Playlist Parser & URL Rewriter
 * Rewrites media segments and sub-manifests to route through the proxy worker.
 */

import { encryptStreamToken } from "./crypto.js";

export async function rewriteHlsManifest(manifestText, targetBaseUrl, proxyOrigin, secret, referer) {
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
