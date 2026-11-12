package com.example.data.repository;

import android.util.Log;
import com.example.config.AppConfig;
import com.example.config.DynamicBackendConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reconstructed TMDB 3.0 API Client (v2.1 Upgraded).
 * Connects to api.themoviedb.org, Hamyra internal backend rails, and worker endpoints.
 */
public final class TmdbApi {
    private static final String TAG = "TmdbApi";

    private static final List<Integer> CURATED_PEOPLE_IDS = Arrays.asList(
            500, 6193, 3223, 1245, 18918, 287, 10859, 90633, 6384, 74568, 505710, 1136406, 35742, 587506, 54693
    );

    public static class MediaItem {
        public final int id;
        public final String title;
        public final String overview;
        public final String posterPath;
        public final String backdropPath;
        public final String releaseDate;
        public final double voteAverage;
        public final String mediaType; // "movie" or "tv"

        public MediaItem(int id, String title, String overview, String posterPath, String backdropPath, String releaseDate, double voteAverage, String mediaType) {
            this.id = id;
            this.title = title;
            this.overview = overview;
            this.posterPath = posterPath;
            this.backdropPath = backdropPath;
            this.releaseDate = releaseDate;
            this.voteAverage = voteAverage;
            this.mediaType = mediaType;
        }

        public String getFullPosterUrl(String size) {
            if (posterPath == null || posterPath.isEmpty()) return null;
            return AppConfig.TMDB_IMAGE_BASE + "/" + (size != null ? size : "w500") + posterPath;
        }
    }

    public static class CastMember {
        public final String name;
        public final String character;
        public final String profileUrl;

        public CastMember(String name, String character, String profileUrl) {
            this.name = name;
            this.character = character;
            this.profileUrl = profileUrl;
        }
    }

    public static class TmdbPerson {
        public final int id;
        public final String name;
        public final String photoUrl;
        public final String role;

        public TmdbPerson(int id, String name, String photoUrl, String role) {
            this.id = id;
            this.name = name;
            this.photoUrl = photoUrl;
            this.role = role;
        }
    }

    public static class Rail {
        public final String key;
        public final String title;
        public final List<MediaItem> items;
        public final String discoverQuery;

        public Rail(String key, String title, List<MediaItem> items, String discoverQuery) {
            this.key = key;
            this.title = title;
            this.items = items;
            this.discoverQuery = discoverQuery;
        }
    }

    /**
     * Search movies or TV shows by text query.
     */
    public static List<MediaItem> search(String query, String type) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        try {
            String endpoint = "movie".equalsIgnoreCase(type) ? "/search/movie" : "/search/tv";
            String url = AppConfig.TMDB_BASE_URL + endpoint + "?api_key=" + AppConfig.TMDB_API_KEY + "&query=" + URLEncoder.encode(query, "UTF-8");
            String json = httpGet(url, null);
            if (json == null) return Collections.emptyList();

            JSONObject root = new JSONObject(json);
            JSONArray results = root.optJSONArray("results");
            if (results == null) return Collections.emptyList();

            List<MediaItem> items = new ArrayList<>(results.length());
            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.optJSONObject(i);
                if (obj == null) continue;

                int id = obj.optInt("id", 0);
                String title = obj.optString("title", obj.optString("name", ""));
                String overview = obj.optString("overview", "");
                String poster = obj.optString("poster_path", null);
                String backdrop = obj.optString("backdrop_path", null);
                String date = obj.optString("release_date", obj.optString("first_air_date", ""));
                double rating = obj.optDouble("vote_average", 0.0);

                if (id > 0 && !title.isEmpty()) {
                    items.add(new MediaItem(id, title, overview, poster, backdrop, date, rating, type));
                }
            }
            return items;
        } catch (Exception e) {
            Log.w(TAG, "Search failed for " + query, e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetch trending movies or shows.
     */
    public static List<MediaItem> getTrending(String mediaType, String timeWindow) {
        String mType = mediaType != null ? mediaType : "movie";
        String window = timeWindow != null ? timeWindow : "week";
        String url = AppConfig.TMDB_BASE_URL + "/trending/" + mType + "/" + window + "?api_key=" + AppConfig.TMDB_API_KEY;
        String json = httpGet(url, null);
        if (json == null) return Collections.emptyList();

        try {
            JSONObject root = new JSONObject(json);
            JSONArray results = root.optJSONArray("results");
            if (results == null) return Collections.emptyList();

            List<MediaItem> items = new ArrayList<>(results.length());
            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.optJSONObject(i);
                if (obj == null) continue;

                int id = obj.optInt("id", 0);
                String title = obj.optString("title", obj.optString("name", ""));
                String poster = obj.optString("poster_path", null);
                String backdrop = obj.optString("backdrop_path", null);
                String date = obj.optString("release_date", obj.optString("first_air_date", ""));
                double rating = obj.optDouble("vote_average", 0.0);

                if (id > 0) {
                    items.add(new MediaItem(id, title, obj.optString("overview", ""), poster, backdrop, date, rating, mType));
                }
            }
            return items;
        } catch (Exception e) {
            Log.w(TAG, "Trending fetch failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetch trending people (Curated actors & directors).
     */
    public static List<TmdbPerson> getTrendingPeople(int limit) {
        List<TmdbPerson> people = new ArrayList<>();
        int count = limit > 0 ? Math.min(limit, CURATED_PEOPLE_IDS.size()) : CURATED_PEOPLE_IDS.size();

        for (int i = 0; i < count; i++) {
            int personId = CURATED_PEOPLE_IDS.get(i);
            try {
                String url = AppConfig.TMDB_BASE_URL + "/person/" + personId + "?api_key=" + AppConfig.TMDB_API_KEY;
                String json = httpGet(url, null);
                if (json == null) continue;

                JSONObject obj = new JSONObject(json);
                if (obj.optBoolean("adult", false)) continue;

                String profile = obj.optString("profile_path", null);
                if (profile == null || profile.isEmpty() || "null".equals(profile)) continue;

                String profileUrl = AppConfig.TMDB_IMAGE_BASE + "/w185" + profile;
                String name = obj.optString("name", "");
                if (name.isEmpty()) continue;

                String dept = obj.optString("known_for_department", "Acting");
                if ("Acting".equalsIgnoreCase(dept)) {
                    dept = "Actor";
                }

                people.add(new TmdbPerson(personId, name, profileUrl, dept));
            } catch (Exception ignored) {}
        }
        return people;
    }

    /**
     * Fetch Hindi Dubbed Anime Rail (NEW in Hamyra 2.1).
     * Connects to internal backend with X-Internal-Auth token.
     */
    public static Rail fetchHindiDubAnimeRail(String kind) {
        String mediaKind = "movie".equalsIgnoreCase(kind) ? "movie" : "tv";
        String backendBase = DynamicBackendConfig.INSTANCE.getBackendUrl();
        String url = backendBase + "/anime/hindi-dub?kind=" + mediaKind + "&limit=40";

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("X-Internal-Auth", AppConfig.INTERNAL_AUTH_KEY);

        String json = httpGet(url, headers);
        if (json == null) return null;

        try {
            JSONObject root = new JSONObject(json);
            if (!root.optBoolean("ok", false)) return null;

            JSONArray itemsArr = root.optJSONArray("items");
            if (itemsArr == null) return null;

            List<MediaItem> items = new ArrayList<>(itemsArr.length());
            for (int i = 0; i < itemsArr.length(); i++) {
                JSONObject obj = itemsArr.optJSONObject(i);
                if (obj == null) continue;

                int tmdbId = obj.optInt("tmdbId", 0);
                if (tmdbId <= 0) continue;

                String title = obj.optString("title", "");
                if (title.isEmpty()) continue;

                String poster = obj.optString("poster_path", null);
                if ("null".equals(poster) || (poster != null && poster.isEmpty())) {
                    poster = null;
                }
                String year = obj.optString("year", "");

                items.add(new MediaItem(tmdbId, title, "", poster, null, year, 0.0, mediaKind));
            }

            if (!items.isEmpty()) {
                return new Rail("hindi-anime", "🍿 Hindi Dubbed Anime", items, "");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse Hindi dub anime rail", e);
        }
        return null;
    }

    /**
     * Fetch cast and credits for a movie or TV show.
     */
    public static List<CastMember> getCredits(int id, String mediaType) {
        String endpoint = "movie".equalsIgnoreCase(mediaType) ? "/movie/" : "/tv/";
        String url = AppConfig.TMDB_BASE_URL + endpoint + id + "/credits?api_key=" + AppConfig.TMDB_API_KEY;
        String json = httpGet(url, null);
        if (json == null) return Collections.emptyList();

        try {
            JSONObject root = new JSONObject(json);
            JSONArray cast = root.optJSONArray("cast");
            if (cast == null) return Collections.emptyList();

            List<CastMember> list = new ArrayList<>(cast.length());
            for (int i = 0; i < cast.length(); i++) {
                JSONObject obj = cast.optJSONObject(i);
                if (obj == null) continue;

                String name = obj.optString("name", "");
                String character = obj.optString("character", "");
                String profile = obj.optString("profile_path", null);
                String profileUrl = profile != null ? AppConfig.TMDB_IMAGE_BASE + "/w185" + profile : null;

                if (!name.isEmpty()) {
                    list.add(new CastMember(name, character, profileUrl));
                }
            }
            return list;
        } catch (Exception e) {
            Log.w(TAG, "Credits fetch failed for " + id, e);
            return Collections.emptyList();
        }
    }

    private static String httpGet(String urlStr, Map<String, String> headers) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
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
