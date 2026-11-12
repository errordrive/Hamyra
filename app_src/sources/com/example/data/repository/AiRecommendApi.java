package com.example.data.repository;

import android.util.Log;
import com.example.config.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reconstructed AI Recommendation Client.
 * Connects to the Cloudflare Worker AI endpoint (/ai/recommend) with HMAC-SHA256 signature,
 * or direct Google Gemini AI fallback.
 */
public final class AiRecommendApi {
    private static final String TAG = "AiRecommendApi";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 20000;

    public static class Item {
        public final int tmdbId;
        public final String kind;
        public final String title;
        public final String year;
        public final String posterUrl;
        public final String reason;
        public final double rating;

        public Item(int tmdbId, String kind, String title, String year, String posterUrl, String reason, double rating) {
            this.tmdbId = tmdbId;
            this.kind = kind;
            this.title = title;
            this.year = year;
            this.posterUrl = posterUrl;
            this.reason = reason;
            this.rating = rating;
        }
    }

    public static abstract class Outcome {
        public static final class Success extends Outcome {
            public final List<Item> items;
            public Success(List<Item> items) { this.items = items; }
        }

        public static final class OffTopic extends Outcome {
            public static final OffTopic INSTANCE = new OffTopic();
            private OffTopic() {}
        }

        public static final class Error extends Outcome {
            public final String message;
            public Error(String message) { this.message = message; }
        }
    }

    /**
     * Request recommendations from the AI worker.
     */
    public static Outcome getRecommendations(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new Outcome.Error("Type a mood, vibe, or title to get started.");
        }

        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String sig = generateHmacSignature(ts);
        if (sig == null) {
            return new Outcome.Error("Couldn't reach the recommendation service.");
        }

        HttpURLConnection conn = null;
        try {
            String endpoint = AppConfig.WORKER_BASE + "/ai/recommend?q=" + URLEncoder.encode(query.trim(), "UTF-8");
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("X-Hamyra-Ts", ts);
            conn.setRequestProperty("X-Hamyra-Sig", sig);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", AppConfig.USER_AGENT_HAMYRA);

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                return new Outcome.Error("Couldn't reach the recommendation service.");
            }

            String response = readStream(is);
            if (code >= 200 && code < 300 && response != null && !response.trim().isEmpty()) {
                JSONObject root = new JSONObject(response);
                if (!root.optBoolean("ok", false)) {
                    String err = root.optString("error", "Something went wrong.");
                    return new Outcome.Error(err);
                }

                if (root.optBoolean("offtopic", false)) {
                    return Outcome.OffTopic.INSTANCE;
                }

                JSONArray results = root.optJSONArray("results");
                if (results == null) results = new JSONArray();

                List<Item> items = new ArrayList<>(results.length());
                for (int i = 0; i < results.length(); i++) {
                    JSONObject obj = results.optJSONObject(i);
                    if (obj == null) continue;

                    int id = obj.optInt("id", 0);
                    String title = obj.optString("title", "");
                    String posterUrl = obj.optString("poster_url", "");

                    if (id > 0 && !title.isEmpty() && !posterUrl.isEmpty()) {
                        items.add(new Item(
                                id,
                                obj.optString("kind", "movie"),
                                title,
                                obj.optString("year", ""),
                                posterUrl,
                                obj.optString("reason", ""),
                                obj.optDouble("rating", 0.0)
                        ));
                    }
                }
                return new Outcome.Success(items);
            }

            return new Outcome.Error("Recommendation service returned code " + code);
        } catch (Exception e) {
            Log.w(TAG, "Recommendation request failed", e);
            return new Outcome.Error("Couldn't reach the recommendation service. Check your connection.");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String generateHmacSignature(String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(AppConfig.HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "HMAC error", e);
            return null;
        }
    }

    private static String readStream(InputStream is) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
