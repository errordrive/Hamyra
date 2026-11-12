package com.example.data.model;

/**
 * Reconstructed Stream Source for a Live Match.
 */
public final class MatchStream {
    public final String title;
    public final String embedUrl;
    public final String referer;
    public final String userAgent;
    public final String quality;

    public MatchStream(String title, String embedUrl, String referer, String userAgent, String quality) {
        this.title = title;
        this.embedUrl = embedUrl;
        this.referer = referer;
        this.userAgent = userAgent;
        this.quality = quality;
    }
}
