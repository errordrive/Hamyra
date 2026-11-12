package com.example.data.model;

import java.util.List;
import java.util.Map;

/**
 * Reconstructed Playable Stream Source model.
 */
public final class PlayableSource {
    public final String name;
    public final String url;
    public final String quality;
    public final String format;
    public final Map<String, String> headers;
    public final List<Subtitle> subtitles;

    public PlayableSource(String name, String url, String quality, String format, Map<String, String> headers, List<Subtitle> subtitles) {
        this.name = name;
        this.url = url;
        this.quality = quality;
        this.format = format;
        this.headers = headers;
        this.subtitles = subtitles;
    }
}
