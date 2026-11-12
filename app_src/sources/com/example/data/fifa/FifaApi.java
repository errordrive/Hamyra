package com.example.data.fifa;

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
 * Reconstructed FIFA & World Cup API Client.
 * Connects to ESPN FIFA World Cup API, FIFA+ CXM API, and Storyteller Highlights.
 */
public final class FifaApi {
    private static final String TAG = "FifaApi";
    private static final String API = "https://api.fifa.com/api/v3";
    private static final String CXM = "https://cxm-api.fifa.com/fifaplusweb/api";
    private static final String ESPN_WC = "https://site.api.espn.com/apis/site/v2/sports/soccer/fifa.world";
    private static final String UA = "Mozilla/5.0 (Linux; Android 13)";

    public static class EspnEvent {
        public final String id;
        public final String date;
        public final String name;
        public final String statusState;
        public final String statusDetail;
        public final String homeName;
        public final String homeAbbr;
        public final String homeLogo;
        public final String homeScore;
        public final String awayName;
        public final String awayAbbr;
        public final String awayLogo;
        public final String awayScore;
        public final String venue;

        public EspnEvent(String id, String date, String name, String statusState, String statusDetail,
                         String homeName, String homeAbbr, String homeLogo, String homeScore,
                         String awayName, String awayAbbr, String awayLogo, String awayScore, String venue) {
            this.id = id;
            this.date = date;
            this.name = name;
            this.statusState = statusState;
            this.statusDetail = statusDetail;
            this.homeName = homeName;
            this.homeAbbr = homeAbbr;
            this.homeLogo = homeLogo;
            this.homeScore = homeScore;
            this.awayName = awayName;
            this.awayAbbr = awayAbbr;
            this.awayLogo = awayLogo;
            this.awayScore = awayScore;
            this.venue = venue;
        }

        public boolean isLive() {
            return "in".equalsIgnoreCase(statusState);
        }

        public boolean isFinished() {
            return "post".equalsIgnoreCase(statusState);
        }

        public boolean isUpcoming() {
            return "pre".equalsIgnoreCase(statusState);
        }
    }

    public static class FifaClip {
        public final String id;
        public final String title;
        public final String videoUrl;
        public final String thumbnailUrl;
        public final long durationMs;

        public FifaClip(String id, String title, String videoUrl, String thumbnailUrl, long durationMs) {
            this.id = id;
            this.title = title;
            this.videoUrl = videoUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.durationMs = durationMs;
        }
    }

    /**
     * Fetch upcoming & live matches from ESPN FIFA endpoint.
     */
    public static List<EspnEvent> fetchEspnSchedule() {
        String json = httpGet(ESPN_WC + "/scoreboard", null);
        if (json == null) return Collections.emptyList();

        try {
            JSONObject root = new JSONObject(json);
            JSONArray events = root.optJSONArray("events");
            if (events == null) return Collections.emptyList();

            List<EspnEvent> list = new ArrayList<>(events.length());
            for (int i = 0; i < events.length(); i++) {
                JSONObject ev = events.optJSONObject(i);
                if (ev == null) continue;

                String id = ev.optString("id");
                String date = ev.optString("date");
                String name = ev.optString("name");

                JSONObject status = ev.optJSONObject("status");
                JSONObject statusType = status != null ? status.optJSONObject("type") : null;
                String state = statusType != null ? statusType.optString("state", "pre") : "pre";
                String detail = statusType != null ? statusType.optString("detail", "") : "";

                JSONArray competitions = ev.optJSONArray("competitions");
                JSONObject comp = competitions != null && competitions.length() > 0 ? competitions.optJSONObject(0) : null;
                JSONArray competitors = comp != null ? comp.optJSONArray("competitors") : null;

                JSONObject home = null, away = null;
                if (competitors != null) {
                    for (int c = 0; c < competitors.length(); c++) {
                        JSONObject cp = competitors.optJSONObject(c);
                        if (cp == null) continue;
                        if ("home".equalsIgnoreCase(cp.optString("homeAway"))) {
                            home = cp;
                        } else {
                            away = cp;
                        }
                    }
                }

                JSONObject homeTeam = home != null ? home.optJSONObject("team") : null;
                JSONObject awayTeam = away != null ? away.optJSONObject("team") : null;

                list.add(new EspnEvent(
                        id,
                        date,
                        name,
                        state,
                        detail,
                        homeTeam != null ? homeTeam.optString("displayName", "") : "",
                        homeTeam != null ? homeTeam.optString("abbreviation", "") : "",
                        homeTeam != null ? homeTeam.optString("logo", "") : "",
                        home != null ? home.optString("score", "0") : "0",
                        awayTeam != null ? awayTeam.optString("displayName", "") : "",
                        awayTeam != null ? awayTeam.optString("abbreviation", "") : "",
                        awayTeam != null ? awayTeam.optString("logo", "") : "",
                        away != null ? away.optString("score", "0") : "0",
                        comp != null && comp.optJSONObject("venue") != null ? comp.optJSONObject("venue").optString("fullName", "") : ""
                ));
            }
            return list;
        } catch (Exception e) {
            Log.w(TAG, "ESPN schedule parse failed", e);
            return Collections.emptyList();
        }
    }

    private static String httpGet(String urlStr, String referer) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "application/json");
            if (referer != null) {
                conn.setRequestProperty("Referer", referer);
            }

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "GET " + urlStr + " failed", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }
}
