package com.example.data.repository;

import android.util.Base64;
import android.util.Log;
import com.example.data.model.PlayableSource;
import com.example.data.model.Subtitle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reconstructed Vidnest Streaming API.
 * Resolves streams from vidnest.fun endpoints with custom decoding.
 */
public final class VidnestApi {
    private static final String TAG = "VidnestApi";
    private static final String BASE = "https://new.vidnest.fun/";
    private static final String REFERER = "https://vidnest.fun/";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    public static class VidnestResult {
        public final boolean success;
        public final List<PlayableSource> sources;
        public final List<Subtitle> subtitles;

        public VidnestResult(boolean success, List<PlayableSource> sources, List<Subtitle> subtitles) {
            this.success = success;
            this.sources = sources != null ? sources : Collections.emptyList();
            this.subtitles = subtitles != null ? subtitles : Collections.emptyList();
        }
    }

    /**
     * Resolve media item from Vidnest.
     */
    public static VidnestResult resolve(String path) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("Referer", REFERER);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                String body = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(body);

                List<PlayableSource> sources = new ArrayList<>();
                JSONArray streamsArr = json.optJSONArray("sources");
                if (streamsArr != null) {
                    for (int i = 0; i < streamsArr.length(); i++) {
                        JSONObject sObj = streamsArr.optJSONObject(i);
                        if (sObj == null) continue;

                        String file = sObj.optString("file");
                        if (!file.isEmpty()) {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Referer", REFERER);
                            headers.put("User-Agent", UA);

                            sources.add(new PlayableSource(
                                    sObj.optString("label", "Vidnest " + (i + 1)),
                                    file,
                                    sObj.optString("type", "mp4"),
                                    "auto",
                                    headers,
                                    null
                            ));
                        }
                    }
                }

                List<Subtitle> subs = new ArrayList<>();
                JSONArray tracks = json.optJSONArray("tracks");
                if (tracks != null) {
                    for (int i = 0; i < tracks.length(); i++) {
                        JSONObject tObj = tracks.optJSONObject(i);
                        if (tObj == null) continue;

                        if ("captions".equalsIgnoreCase(tObj.optString("kind"))) {
                            subs.add(new Subtitle(
                                    tObj.optString("file"),
                                    tObj.optString("label", "Unknown"),
                                    tObj.optString("language", "en"),
                                    tObj.optBoolean("default", false)
                            ));
                        }
                    }
                }

                return new VidnestResult(true, sources, subs);
            }
        } catch (Exception e) {
            Log.w(TAG, "Vidnest resolution failed", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return new VidnestResult(false, Collections.emptyList(), Collections.emptyList());
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
