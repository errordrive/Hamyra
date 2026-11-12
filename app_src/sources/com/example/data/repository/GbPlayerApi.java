package com.example.data.repository;

import android.util.Log;
import com.example.data.model.PlayableSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Reconstructed GbPlayer & Rutube Fallback Stream Client.
 * Provides fallback HLS feeds for cricket and international sports.
 */
public final class GbPlayerApi {
    private static final String TAG = "GbPlayerApi";
    public static final String CDN_PRIMARY = "https://bhotasportzx.xyz/video/sportzfy-hls/index.m3u8";
    private static final String CDN_REFERER = "https://bhotasportzx.xyz/";
    private static final String UA = "okhttp/4.12.0";

    // Rutube Resolution Variants
    public static final Map<String, String> RUTUBE_STREAMS = new LinkedHashMap<>();
    static {
        RUTUBE_STREAMS.put("1080p HD", "https://river-1.rutube.ru/stream/genetta-421.ntv.rutube.ru/AmlrArkBYEBfSGRgVzdm5w/1783161432/7c13a51576b9ff2601f08f5d57dd5169/1080p_stream.m3u8");
        RUTUBE_STREAMS.put("720p", "https://river-1.rutube.ru/stream/genetta-421.ntv.rutube.ru/AmlrArkBYEBfSGRgVzdm5w/1783161432/7c13a51576b9ff2601f08f5d57dd5169/720p_stream.m3u8");
        RUTUBE_STREAMS.put("360p", "https://river-1.rutube.ru/stream/genetta-421.ntv.rutube.ru/AmlrArkBYEBfSGRgVzdm5w/1783161432/7c13a51576b9ff2601f08f5d57dd5169/360p_stream.m3u8");
    }

    /**
     * Get primary fallback HLS stream with headers.
     */
    public static PlayableSource getPrimaryFallbackStream() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", CDN_REFERER);
        headers.put("User-Agent", UA);
        return new PlayableSource("Sportzfy Primary Stream", CDN_PRIMARY, "1080p", "application/x-mpegURL", headers, null);
    }

    /**
     * Get Rutube variant streams.
     */
    public static List<PlayableSource> getRutubeStreams() {
        List<PlayableSource> list = new ArrayList<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", UA);

        for (Map.Entry<String, String> entry : RUTUBE_STREAMS.entrySet()) {
            list.add(new PlayableSource(
                    "Rutube Live (" + entry.getKey() + ")",
                    entry.getValue(),
                    entry.getKey(),
                    "application/x-mpegURL",
                    headers,
                    null
            ));
        }
        return list;
    }
}
