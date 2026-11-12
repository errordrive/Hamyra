package com.example.data.repository;

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
 * Reconstructed Flaxfer / Multi-Provider Movie Streaming Resolver.
 * Resolves streams and subtitles across Stela, VZ, and Freaky resolvers.
 */
public final class FlaxferApi {
    private static final String TAG = "FlaxferApi";
    private static final String FREAKY_DIRECT = "https://freakyniki.elaxo.lol/";
    private static final String FREAKY_WORKER = "https://hamyra-api.aininjaibrahim.workers.dev/tmdb/flaxfer";
    private static final String STELA = "https://stela.raphsm4.dev/resolve";
    private static final String VZ = "https://api.vz.raphsm4.dev/resolve";

    public static class Result {
        public final boolean ok;
        public final List<PlayableSource> streams;
        public final List<Subtitle> subtitles;

        public Result(boolean ok, List<PlayableSource> streams, List<Subtitle> subtitles) {
            this.ok = ok;
            this.streams = streams != null ? streams : Collections.emptyList();
            this.subtitles = subtitles != null ? subtitles : Collections.emptyList();
        }
    }

    /**
     * Resolve streams for a movie or TV show episode.
     */
    public static Result resolve(int tmdbId, String mediaType, Integer season, Integer episode) {
        String query;
        if ("tv".equalsIgnoreCase(mediaType) && season != null && episode != null) {
            query = "?tmdbId=" + tmdbId + "&type=tv&season=" + season + "&episode=" + episode;
        } else {
            query = "?tmdbId=" + tmdbId + "&type=movie";
        }

        // Try Freaky Worker primary
        Result res = fetchFromEndpoint(FREAKY_WORKER + query);
        if (res.ok && !res.streams.isEmpty()) {
            return res;
        }

        // Fallback to Stela
        res = fetchFromEndpoint(STELA + query);
        if (res.ok && !res.streams.isEmpty()) {
            return res;
        }

        // Fallback to VZ
        return fetchFromEndpoint(VZ + query);
    }

    private static Result fetchFromEndpoint(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Origin", "https://hamyra.xyz");
            conn.setRequestProperty("Referer", "https://hamyra.xyz/");
            conn.setRequestProperty("User-Agent", "HamyraAndroid/1.0");

            if (conn.getResponseCode() == 200) {
                String body = readStream(conn.getInputStream());
                JSONObject root = new JSONObject(body);

                List<PlayableSource> streams = new ArrayList<>();
                JSONArray streamsArr = root.optJSONArray("streams");
                if (streamsArr != null) {
                    for (int i = 0; i < streamsArr.length(); i++) {
                        JSONObject sObj = streamsArr.optJSONObject(i);
                        if (sObj == null) continue;

                        String sUrl = sObj.optString("url");
                        if (sUrl.isEmpty()) continue;

                        streams.add(new PlayableSource(
                                sObj.optString("name", "Server " + (i + 1)),
                                sUrl,
                                sObj.optString("quality", "Auto"),
                                sObj.optString("format", "hls"),
                                Collections.singletonMap("Referer", "https://flaxfer.lol/"),
                                null
                        ));
                    }
                }

                List<Subtitle> subs = new ArrayList<>();
                JSONArray subsArr = root.optJSONArray("subtitles");
                if (subsArr != null) {
                    for (int i = 0; i < subsArr.length(); i++) {
                        JSONObject subObj = subsArr.optJSONObject(i);
                        if (subObj == null) continue;

                        String subUrl = subObj.optString("url");
                        if (subUrl.isEmpty()) continue;

                        subs.add(new Subtitle(
                                subUrl,
                                subObj.optString("label", "Unknown"),
                                subObj.optString("lang", "en"),
                                subObj.optBoolean("default", false)
                        ));
                    }
                }

                return new Result(true, streams, subs);
            }
        } catch (Exception e) {
            Log.w(TAG, "Resolve failed on " + urlStr, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return new Result(false, Collections.emptyList(), Collections.emptyList());
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
