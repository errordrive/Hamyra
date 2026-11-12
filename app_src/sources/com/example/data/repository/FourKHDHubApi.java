package com.example.data.repository;

import com.example.config.AppConfig;
import com.example.config.DynamicBackendConfig;
import com.example.data.model.PlayableSource;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Reconstructed 4K HD Hub API client (introduced in Hamyra 2.1).
 * Resolves ultra-HD / 4K / 1080p stream links through Cloudflare Worker endpoint (/tmdb/4khdhub).
 */
public final class FourKHDHubApi {
    private static final String TAG = "FourKHDHubApi";
    public static final FourKHDHubApi INSTANCE = new FourKHDHubApi();

    private static final List<String> QUALITY_ORDER = Arrays.asList("1080p", "2160p", "720p", "480p");

    public static class StreamPick {
        public final String url;
        public final String quality;
        public final String language;
        public final String size;

        public StreamPick(String url, String quality, String language, String size) {
            this.url = url;
            this.quality = quality;
            this.language = language;
            this.size = size;
        }
    }

    private FourKHDHubApi() {}

    /**
     * Scores stream host domains for best CDN delivery:
     * 0: Cloudflare workers / R2 dev (Fastest, zero throttling)
     * 1: Hubcloud
     * 2: Pixeldrain
     * 3: Other hosts
     */
    private static int scoreDomain(String url) {
        if (url == null) return 3;
        String lower = url.toLowerCase();
        if (lower.contains(".workers.dev") || lower.contains("r2.dev") || lower.contains("r2.cloudflarestorage")) {
            return 0;
        }
        if (lower.contains("hubcloud")) {
            return 1;
        }
        if (lower.contains("pixeldrain")) {
            return 2;
        }
        return 3;
    }

    /**
     * Resolves playable stream sources for a given movie or TV episode.
     *
     * @param tmdbId TMDB identifier
     * @param type "movie" or "tv"
     * @param season Season number (for TV)
     * @param episode Episode number (for TV)
     * @return List of PlayableSource ordered by quality and optimal host
     */
    public List<PlayableSource> resolve(int tmdbId, String type, String season, String episode) {
        List<PlayableSource> results = new ArrayList<>();
        if (tmdbId <= 0) return results;

        StringBuilder sb = new StringBuilder("?tmdbId=").append(tmdbId).append("&type=").append(type);
        if ("tv".equalsIgnoreCase(type)) {
            sb.append("&s=").append((season == null || season.trim().isEmpty()) ? "1" : season);
            sb.append("&e=").append((episode == null || episode.trim().isEmpty()) ? "1" : episode);
        }

        try {
            String baseUrl = DynamicBackendConfig.INSTANCE.getWorkerUrl();
            URL url = new URL(baseUrl + "/tmdb/4khdhub" + sb.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(25000);
            conn.setRequestProperty("User-Agent", AppConfig.USER_AGENT_MOBILE);
            conn.setRequestProperty("Origin", "https://hamyra.xyz");
            conn.setRequestProperty("Referer", "https://hamyra.xyz/");

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"), 8192);
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(response.toString());
                if (!root.optBoolean("ok", false)) {
                    return results;
                }

                JSONArray streams = root.optJSONArray("streams");
                if (streams == null) return results;

                // Deduplicate & select best host per quality tier
                Map<String, StreamPick> bestPicks = new LinkedHashMap<>();
                for (int i = 0; i < streams.length(); i++) {
                    JSONObject item = streams.optJSONObject(i);
                    if (item == null) continue;

                    String streamUrl = item.optString("url", "").trim();
                    if (streamUrl.isEmpty()) continue;

                    String quality = item.optString("quality", "HD").trim();
                    String lang = item.optString("language", "");
                    String size = item.optString("size", "");

                    StreamPick existing = bestPicks.get(quality);
                    if (existing == null || scoreDomain(streamUrl) < scoreDomain(existing.url)) {
                        bestPicks.put(quality, new StreamPick(streamUrl, quality, lang, size));
                    }
                }

                // Sort by preferred quality order
                List<Map.Entry<String, StreamPick>> entries = new ArrayList<>(bestPicks.entrySet());
                entries.sort((a, b) -> {
                    int indexA = QUALITY_ORDER.indexOf(a.getKey());
                    int indexB = QUALITY_ORDER.indexOf(b.getKey());
                    if (indexA < 0) indexA = QUALITY_ORDER.size();
                    if (indexB < 0) indexB = QUALITY_ORDER.size();
                    return Integer.compare(indexA, indexB);
                });

                for (Map.Entry<String, StreamPick> entry : entries) {
                    StreamPick pick = entry.getValue();
                    String format = pick.url.toLowerCase().endsWith(".mp4") ? "MP4" : "MKV";
                    results.add(new PlayableSource(
                            "4khdhub (" + pick.quality + (pick.size.isEmpty() ? "" : " - " + pick.size) + ")",
                            pick.url,
                            pick.quality,
                            format,
                            null,
                            null
                    ));
                }
            }
        } catch (Throwable t) {
            // Log or fallback
        }

        return results;
    }
}
