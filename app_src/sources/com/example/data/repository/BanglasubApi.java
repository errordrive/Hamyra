package com.example.data.repository;

import android.util.Log;
import com.example.config.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reconstructed Banglasub API Client.
 * Fetches Bangla subtitle series and episode streams from the secondary Cloudflare worker.
 */
public final class BanglasubApi {
    private static final String TAG = "BanglasubApi";
    private static final String BASE = AppConfig.SECONDARY_WORKER_BASE;
    private static final String UA = AppConfig.USER_AGENT_MOBILE;

    private static volatile String cachedToken = null;
    private static volatile long tokenExpiryMs = 0;

    public static class Episode {
        public final int season;
        public final int volume;
        public final String title;
        public final String url;
        public final String poster;

        public Episode(int season, int volume, String title, String url, String poster) {
            this.season = season;
            this.volume = volume;
            this.title = title;
            this.url = url;
            this.poster = poster;
        }
    }

    public static class Series {
        public final String key;
        public final String title;
        public final String poster;
        public final List<Episode> episodes;

        public Series(String key, String title, String poster, List<Episode> episodes) {
            this.key = key;
            this.title = title;
            this.poster = poster;
            this.episodes = episodes != null ? episodes : Collections.emptyList();
        }
    }

    /**
     * Obtain or refresh bearer auth token from the worker.
     */
    private static synchronized String getAuthToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiryMs - 60000L) {
            return cachedToken;
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE + "/auth/token");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("Origin", "https://hamyra.xyz");
            conn.setRequestProperty("Referer", "https://hamyra.xyz/");
            conn.setRequestProperty("User-Agent", UA);

            if (conn.getResponseCode() == 200) {
                String body = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(body);
                String tok = json.optString("token", null);
                long exp = json.optLong("expires_in", 3600L);
                if (tok != null) {
                    cachedToken = tok;
                    tokenExpiryMs = now + (exp * 1000L);
                    return tok;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Auth token fetch failed", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * Fetch list of available Bangla subtitle series.
     */
    public static List<Series> fetchSeries() {
        String token = getAuthToken();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE + "/banglasub/catalog");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Origin", "https://hamyra.xyz");
            conn.setRequestProperty("Referer", "https://hamyra.xyz/");
            conn.setRequestProperty("User-Agent", UA);
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (conn.getResponseCode() == 200) {
                String body = readStream(conn.getInputStream());
                JSONArray arr = new JSONArray(body);
                List<Series> list = new ArrayList<>(arr.length());

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj == null) continue;

                    String key = obj.optString("key");
                    String title = obj.optString("title");
                    String poster = obj.optString("poster");

                    List<Episode> episodes = new ArrayList<>();
                    JSONArray epArr = obj.optJSONArray("episodes");
                    if (epArr != null) {
                        for (int e = 0; e < epArr.length(); e++) {
                            JSONObject epObj = epArr.optJSONObject(e);
                            if (epObj == null) continue;
                            episodes.add(new Episode(
                                    epObj.optInt("season", 1),
                                    epObj.optInt("volume", e + 1),
                                    epObj.optString("title", "Episode " + (e + 1)),
                                    epObj.optString("url"),
                                    epObj.optString("poster", poster)
                            ));
                        }
                    }

                    list.add(new Series(key, title, poster, episodes));
                }
                return list;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to fetch series catalog", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return Collections.emptyList();
    }

    private static String readStream(java.io.InputStream is) throws Exception {
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
