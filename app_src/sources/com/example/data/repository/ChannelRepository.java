package com.example.data.repository;

import android.content.Context;
import com.example.config.AppConfig;
import com.example.data.model.ChannelItem;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconstructed Channel Repository.
 * Manages live TV channels, proxy URL rewriting, and Toffee cookie integration.
 */
public final class ChannelRepository {
    private static final Pattern URL_PARAM_PATTERN = Pattern.compile("[?&]url=([^&]+)");
    private volatile List<ChannelItem> cachedChannels = new ArrayList<>();

    public ChannelRepository() {}

    /**
     * Rewrite channel stream URLs, substituting {{w}} placeholders with the Cloudflare Worker proxy.
     */
    public static List<ChannelItem> processChannelUrls(List<ChannelItem> rawChannels) {
        String workerBase = AppConfig.PROXY_BASE_URL;
        if (workerBase.endsWith("/")) {
            workerBase = workerBase.substring(0, workerBase.length() - 1);
        }

        List<ChannelItem> processed = new ArrayList<>(rawChannels.size());
        for (ChannelItem channel : rawChannels) {
            String streamUrl = channel.streamUrl;
            if (streamUrl == null) {
                processed.add(channel);
                continue;
            }

            if (streamUrl.contains("toffee=1")) {
                Matcher matcher = URL_PARAM_PATTERN.matcher(streamUrl);
                if (matcher.find()) {
                    try {
                        String encodedTarget = matcher.group(1);
                        String decoded = URLDecoder.decode(encodedTarget, StandardCharsets.UTF_8.name());
                        channel = channel.withStreamUrl(decoded);
                    } catch (Exception ignored) {
                        channel = channel.withStreamUrl(streamUrl.replace("{{w}}", workerBase));
                    }
                } else {
                    channel = channel.withStreamUrl(streamUrl.replace("{{w}}", workerBase));
                }
            } else if (streamUrl.contains("{{w}}")) {
                channel = channel.withStreamUrl(streamUrl.replace("{{w}}", workerBase));
            }

            processed.add(channel);
        }

        return processed;
    }

    /**
     * Refresh channels from the Cloudflare Worker API.
     */
    public List<ChannelItem> refreshChannels(Context context) {
        BackendChannels.Result result = BackendChannels.fetch(context);
        List<ChannelItem> processed = processChannelUrls(result.channels);
        this.cachedChannels = processed;
        return processed;
    }

    public List<ChannelItem> getChannels() {
        return this.cachedChannels;
    }

    /**
     * Filter channels by category.
     */
    public List<ChannelItem> getChannelsByCategory(String category) {
        List<ChannelItem> filtered = new ArrayList<>();
        for (ChannelItem item : this.cachedChannels) {
            if (category == null || category.equalsIgnoreCase(item.category)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * Extract distinct categories.
     */
    public Set<String> getCategories() {
        Set<String> categories = new LinkedHashSet<>();
        for (ChannelItem item : this.cachedChannels) {
            if (item.category != null) {
                categories.add(item.category);
            }
        }
        return categories;
    }
}
