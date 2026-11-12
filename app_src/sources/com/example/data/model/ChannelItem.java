package com.example.data.model;

import java.util.Map;
import java.util.Objects;

/**
 * Reconstructed Channel Item Model.
 */
public final class ChannelItem {
    public final String id;
    public final String name;
    public final String streamUrl;
    public final String logoUrl;
    public final String category;
    public final String curProgram;
    public final String curProgramTime;
    public final String viewers;
    public final boolean isLive;
    public final String description;
    public final String resolution;
    public final String bitrate;
    public final String toffeeId;
    public final boolean isFifa;
    public final String referer;
    public final boolean needsToffeeCookie;
    public final String drmScheme;
    public final Map<String, String> drmKeys;

    public ChannelItem(
            String id,
            String name,
            String streamUrl,
            String logoUrl,
            String category,
            String curProgram,
            String curProgramTime,
            String viewers,
            boolean isLive,
            String description,
            String resolution,
            String bitrate,
            String toffeeId,
            boolean isFifa,
            String referer,
            boolean needsToffeeCookie,
            String drmScheme,
            Map<String, String> drmKeys
    ) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.streamUrl = streamUrl != null ? streamUrl : "";
        this.logoUrl = logoUrl != null ? logoUrl : "";
        this.category = category != null ? category : "Live TV";
        this.curProgram = curProgram != null ? curProgram : "Live Show";
        this.curProgramTime = curProgramTime != null ? curProgramTime : "NOW";
        this.viewers = viewers != null ? viewers : "15K viewers";
        this.isLive = isLive;
        this.description = description != null ? description : "Live Streaming Channel";
        this.resolution = resolution != null ? resolution : "1080p 60fps";
        this.bitrate = bitrate != null ? bitrate : "Auto";
        this.toffeeId = toffeeId;
        this.isFifa = isFifa;
        this.referer = referer;
        this.needsToffeeCookie = needsToffeeCookie;
        this.drmScheme = drmScheme;
        this.drmKeys = drmKeys;
    }

    public ChannelItem withStreamUrl(String newStreamUrl) {
        return new ChannelItem(
                this.id,
                this.name,
                newStreamUrl,
                this.logoUrl,
                this.category,
                this.curProgram,
                this.curProgramTime,
                this.viewers,
                this.isLive,
                this.description,
                this.resolution,
                this.bitrate,
                this.toffeeId,
                this.isFifa,
                this.referer,
                this.needsToffeeCookie,
                this.drmScheme,
                this.drmKeys
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelItem that = (ChannelItem) o;
        return Objects.equals(id, that.id) && Objects.equals(streamUrl, that.streamUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, streamUrl);
    }
}
