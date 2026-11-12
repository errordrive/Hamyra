package com.example.data.repository;

import com.example.config.AppConfig;
import com.example.data.model.Subtitle;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Reconstructed Bangla Subtitle API Client (BnSubApi, introduced in Hamyra 2.1).
 * Fetches Bangla subtitle track (*.vtt) via https://px.talkoraai.com/bnsub/get.
 */
public final class BnSubApi {
    private static final String TAG = "BnSubApi";
    public static final BnSubApi INSTANCE = new BnSubApi();

    private static final String BASE_URL = AppConfig.BNSUB_BASE_URL;

    private BnSubApi() {}

    /**
     * Fetches a matching Bengali subtitle for the specified title.
     *
     * @param title Title of movie or TV show
     * @param type "movie" or "tv"
     * @param tmdbId TMDB ID (optional)
     * @param year Release year (optional)
     * @param season Season number (optional, for TV)
     * @param episode Episode number (optional, for TV)
     * @return Subtitle object or null if none found
     */
    public Subtitle fetch(String title, String type, Integer tmdbId, Integer year, Integer season, Integer episode) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        try {
            StringBuilder sb = new StringBuilder("?title=");
            sb.append(URLEncoder.encode(title.trim(), "UTF-8"));
            sb.append("&type=").append(type != null ? type : "movie");

            if (tmdbId != null && tmdbId > 0) {
                sb.append("&tmdbId=").append(tmdbId);
            }
            if (year != null && year > 0) {
                sb.append("&year=").append(year);
            }
            if (season != null && season > 0) {
                sb.append("&s=").append(season);
            }
            if (episode != null && episode > 0) {
                sb.append("&e=").append(episode);
            }

            URL url = new URL(BASE_URL + "/bnsub/get" + sb.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(45000);
            conn.setRequestProperty("User-Agent", AppConfig.USER_AGENT_MOBILE);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"), 8192);
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.optBoolean("ok", false)) {
                    String subUrl = json.optString("url", "").trim();
                    if (!subUrl.isEmpty()) {
                        String label = json.optString("label", "বাংলা");
                        if (label.isEmpty()) label = "বাংলা";
                        return new Subtitle(subUrl, label, "bn", true);
                    }
                }
            }
        } catch (Throwable t) {
            // Ignored or logged
        }
        return null;
    }
}
