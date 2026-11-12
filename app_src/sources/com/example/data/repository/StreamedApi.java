package com.example.data.repository;

import android.util.Log;
import com.example.config.AppConfig;
import com.example.data.model.MatchStatus;
import com.example.data.model.MatchStream;
import com.example.data.model.SportCategory;
import com.example.data.model.SportMatch;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reconstructed StreamedApi client.
 * Connects to streamed.pk to fetch live sports matches and resolve stream embeds.
 */
public final class StreamedApi {
    private static final String TAG = "StreamedApi";
    private static final String BASE = "https://streamed.pk/api";
    private static final String UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/110.0.0.0 Mobile Safari/537.36";
    private static final List<String> SPORTS = Arrays.asList("football", "cricket", "motor-sports", "fight", "basketball");

    private static final Map<String, SportCategory> SPORT_MAP = new HashMap<>();
    static {
        SPORT_MAP.put("football", SportCategory.FOOTBALL);
        SPORT_MAP.put("cricket", SportCategory.CRICKET);
        SPORT_MAP.put("motor-sports", SportCategory.MOTORSPORT);
        SPORT_MAP.put("fight", SportCategory.WRESTLING);
        SPORT_MAP.put("basketball", SportCategory.OTHER);
        SPORT_MAP.put("tennis", SportCategory.TENNIS);
    }

    /**
     * Fetch all live and upcoming matches across primary sports.
     */
    public static List<SportMatch> fetchAllMatches() {
        List<SportMatch> all = new ArrayList<>();
        for (String sport : SPORTS) {
            try {
                all.addAll(fetchSportMatches(sport));
            } catch (Exception e) {
                Log.w(TAG, "Error fetching sport " + sport, e);
            }
        }
        return all;
    }

    /**
     * Fetch matches for a specific sport category.
     */
    public static List<SportMatch> fetchSportMatches(String sport) {
        String urlStr = BASE + "/matches/" + sport;
        String rawJson = httpGet(urlStr);
        if (rawJson == null) return Collections.emptyList();

        try {
            JSONArray arr = new JSONArray(rawJson);
            long now = System.currentTimeMillis();
            List<SportMatch> matches = new ArrayList<>(arr.length());

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;

                String id = obj.optString("id", null);
                if (id == null || id.isEmpty()) continue;

                String title = obj.optString("title", "");
                String categoryStr = obj.optString("category", sport);
                long dateMs = obj.optLong("date", 0L);

                String poster = obj.optString("poster", null);
                if (poster != null && !poster.startsWith("http")) {
                    poster = "https://streamed.pk" + poster;
                }

                JSONObject teamsObj = obj.optJSONObject("teams");
                JSONObject homeObj = teamsObj != null ? teamsObj.optJSONObject("home") : null;
                JSONObject awayObj = teamsObj != null ? teamsObj.optJSONObject("away") : null;

                String homeName = homeObj != null ? homeObj.optString("name") : "";
                String awayName = awayObj != null ? awayObj.optString("name") : "";

                if (homeName.isEmpty() && title.contains(" vs ")) {
                    homeName = title.substring(0, title.indexOf(" vs "));
                }
                if (awayName.isEmpty() && title.contains(" vs ")) {
                    awayName = title.substring(title.indexOf(" vs ") + 4);
                }

                MatchStatus status = MatchStatus.LIVE;
                if (dateMs != 0) {
                    if (dateMs < now - 14400000L) {
                        status = MatchStatus.RECENT;
                    } else if (dateMs >= now + 300000L) {
                        status = MatchStatus.UPCOMING;
                    }
                }

                List<String> rawSources = new ArrayList<>();
                JSONArray sourcesArr = obj.optJSONArray("sources");
                if (sourcesArr != null) {
                    for (int s = 0; s < sourcesArr.length(); s++) {
                        JSONObject sObj = sourcesArr.optJSONObject(s);
                        if (sObj != null) {
                            String src = sObj.optString("source");
                            String sId = sObj.optString("id");
                            if (!src.isEmpty() && !sId.isEmpty()) {
                                rawSources.add(src + "|" + sId);
                            }
                        }
                    }
                }

                SportCategory cat = SPORT_MAP.get(categoryStr.toLowerCase(Locale.ROOT));
                if (cat == null) cat = SportCategory.OTHER;

                String homeShort = homeName.length() >= 3 ? homeName.substring(0, 3).toUpperCase(Locale.ROOT) : homeName.toUpperCase(Locale.ROOT);
                String awayShort = awayName.length() >= 3 ? awayName.substring(0, 3).toUpperCase(Locale.ROOT) : awayName.toUpperCase(Locale.ROOT);

                matches.add(new SportMatch(
                        "streamed-" + id,
                        cat,
                        title,
                        new SportMatch.TeamInfo(homeName, homeShort, poster),
                        new SportMatch.TeamInfo(awayName, awayShort, null),
                        dateMs > 0 ? dateMs : now,
                        status,
                        rawSources,
                        status == MatchStatus.LIVE && dateMs > 0 ? (now - dateMs) / 1000 : null
                ));
            }

            return matches;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse matches for " + sport, e);
            return Collections.emptyList();
        }
    }

    /**
     * Resolve playable stream links for a source token (e.g. "alpha|12345").
     */
    public static List<MatchStream> resolveStream(String sourceToken) {
        if (sourceToken == null || !sourceToken.contains("|")) return Collections.emptyList();

        String[] parts = sourceToken.split("\\|", 2);
        String source = parts[0];
        String streamId = parts[1];

        String url = BASE + "/stream/" + source + "/" + streamId;
        String rawJson = httpGet(url);
        if (rawJson == null) return Collections.emptyList();

        try {
            JSONArray arr = new JSONArray(rawJson);
            List<MatchStream> streams = new ArrayList<>(arr.length());

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;

                String embedUrl = obj.optString("embedUrl", null);
                if (embedUrl == null || embedUrl.isEmpty()) continue;

                int streamNo = obj.optInt("streamNo", i + 1);
                String lang = obj.optString("language", "Source " + streamNo);
                boolean isHd = obj.optBoolean("hd", false);

                String formattedTitle = source.toUpperCase(Locale.ROOT) + " · " + lang + (isHd ? " · HD" : "");
                streams.add(new MatchStream(
                        formattedTitle,
                        embedUrl,
                        "https://streamed.pk/",
                        UA,
                        isHd ? "HD" : "SD"
                ));
            }

            return streams;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse stream resolution", e);
            return Collections.emptyList();
        }
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "application/json");

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
