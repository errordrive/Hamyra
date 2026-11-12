package com.example.data.repository;

import android.content.Context;
import android.util.Log;
import com.example.config.AppConfig;
import com.example.data.model.ChannelItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reconstructed BackendChannels API.
 * Interacts with the Cloudflare Worker channels endpoint using HMAC-SHA256 request signing.
 */
public final class BackendChannels {
    private static final String TAG = "BackendChannels";
    private static final String CACHE_FILE = "channels-cache.json";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;

    public static final class Result {
        public final List<ChannelItem> channels;
        public final boolean fromCache;
        public final String updatedAt;

        public Result(List<ChannelItem> channels, boolean fromCache, String updatedAt) {
            this.channels = channels != null ? channels : Collections.emptyList();
            this.fromCache = fromCache;
            this.updatedAt = updatedAt;
        }
    }

    /**
     * Compute HMAC-SHA256 signature for the given timestamp.
     */
    public static String generateSignature(String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(AppConfig.HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(rawHmac.length * 2);
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "HMAC signature computation failed", e);
            return null;
        }
    }

    /**
     * Fetch channels from remote backend or fallback to local disk cache.
     */
    public static Result fetch(Context context) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = generateSignature(timestamp);
        if (signature == null) {
            return loadCache(context);
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(AppConfig.CHANNELS_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("X-Hamyra-Ts", timestamp);
            conn.setRequestProperty("X-Hamyra-Sig", signature);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", AppConfig.USER_AGENT_HAMYRA);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String responseBody = readStream(conn.getInputStream());
                Result result = parseJson(responseBody, false);
                saveCache(context, responseBody);
                return result;
            } else {
                Log.w(TAG, "Worker responded with HTTP " + responseCode);
                return loadCache(context);
            }
        } catch (Exception e) {
            Log.w(TAG, "Network fetch failed, falling back to cache", e);
            return loadCache(context);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Parse raw JSON response into ChannelItem models.
     */
    public static Result parseJson(String jsonStr, boolean fromCache) throws JSONException {
        JSONObject root = new JSONObject(jsonStr);
        String updatedAt = root.optString("updated_at", null);
        if (updatedAt != null && updatedAt.trim().isEmpty()) {
            updatedAt = null;
        }

        JSONArray channelsArray = root.getJSONArray("channels");
        List<ChannelItem> items = new ArrayList<>(channelsArray.length());

        for (int i = 0; i < channelsArray.length(); i++) {
            JSONObject obj = channelsArray.getJSONObject(i);
            String id = obj.optString("id");
            String name = obj.optString("name");
            String streamUrl = obj.optString("streamUrl");

            if (id.isEmpty() || name.isEmpty() || streamUrl.isEmpty()) {
                continue;
            }

            Map<String, String> drmKeys = null;
            JSONObject drmObj = obj.optJSONObject("drmKeys");
            if (drmObj != null) {
                drmKeys = new LinkedHashMap<>();
                Iterator<String> keys = drmObj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    String v = drmObj.optString(k);
                    if (!v.isEmpty()) {
                        drmKeys.put(k, v);
                    }
                }
            }

            items.add(new ChannelItem(
                    id,
                    name,
                    streamUrl,
                    obj.optString("logoUrl", ""),
                    obj.optString("category", "Live TV"),
                    obj.optString("curProgram", "Live Show"),
                    obj.optString("curProgramTime", "NOW"),
                    obj.optString("viewers", "15K viewers"),
                    obj.optBoolean("isLive", true),
                    obj.optString("description", "Live Streaming Channel"),
                    obj.optString("resolution", "1080p 60fps"),
                    obj.optString("bitrate", "Auto"),
                    obj.optString("toffeeId", null),
                    obj.optBoolean("fifa", false),
                    obj.optString("referer", null),
                    obj.optBoolean("needsToffeeCookie", false),
                    obj.optString("drmScheme", null),
                    drmKeys
            ));
        }

        return new Result(items, fromCache, updatedAt);
    }

    private static void saveCache(Context context, String jsonStr) {
        if (context == null) return;
        try {
            File cacheFile = new File(context.getFilesDir(), CACHE_FILE);
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(jsonStr.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Log.w(TAG, "Cache write error", e);
        }
    }

    public static Result loadCache(Context context) {
        if (context == null) return new Result(Collections.emptyList(), true, null);
        try {
            File cacheFile = new File(context.getFilesDir(), CACHE_FILE);
            if (!cacheFile.exists()) return new Result(Collections.emptyList(), true, null);

            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                String jsonStr = readStream(fis);
                return parseJson(jsonStr, true);
            }
        } catch (Exception e) {
            Log.w(TAG, "Cache read error", e);
            return new Result(Collections.emptyList(), true, null);
        }
    }

    private static String readStream(InputStream is) throws IOException {
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
