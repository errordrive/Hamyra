package com.example.data.repository;

import android.util.Log;
import com.example.data.model.PlayableSource;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reconstructed MovieBox & Cinejoy API client.
 * Connects to downloads.wing.st / cinejoy.to to extract high-speed streaming links.
 */
public final class MovieBoxApi {
    private static final String TAG = "MovieBoxApi";
    private static final String BASE = "https://downloads.wing.st";
    private static final String ORIGIN = "https://cinejoy.to";
    private static final String UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36";

    /**
     * Resolve playable movie sources from MovieBox / Wing.st
     */
    public static List<PlayableSource> resolveMovie(String slug) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE + "/api/v1/movie/" + slug);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Origin", ORIGIN);
            conn.setRequestProperty("User-Agent", UA);

            if (conn.getResponseCode() == 200) {
                String body = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(body);
                JSONArray sourcesArr = json.optJSONArray("sources");
                if (sourcesArr == null) return Collections.emptyList();

                List<PlayableSource> list = new ArrayList<>(sourcesArr.length());
                for (int i = 0; i < sourcesArr.length(); i++) {
                    JSONObject obj = sourcesArr.optJSONObject(i);
                    if (obj == null) continue;

                    String file = obj.optString("file");
                    if (!file.isEmpty()) {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Origin", ORIGIN);
                        headers.put("User-Agent", UA);

                        list.add(new PlayableSource(
                                obj.optString("label", "MovieBox Server " + (i + 1)),
                                file,
                                obj.optString("quality", "1080p"),
                                obj.optString("type", "mp4"),
                                headers,
                                null
                        ));
                    }
                }
                return list;
            }
        } catch (Exception e) {
            Log.w(TAG, "MovieBox resolve failed for " + slug, e);
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
