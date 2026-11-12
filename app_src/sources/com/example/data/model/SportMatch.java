package com.example.data.model;

import java.util.List;

/**
 * Reconstructed Sport Match representation.
 */
public final class SportMatch {
    public final String id;
    public final SportCategory category;
    public final String title;
    public final TeamInfo homeTeam;
    public final TeamInfo awayTeam;
    public final long kickoffTimestampMs;
    public final MatchStatus status;
    public final List<String> rawSources;
    public final Long elapsedSeconds;

    public static class TeamInfo {
        public final String name;
        public final String shortCode;
        public final String badgeUrl;

        public TeamInfo(String name, String shortCode, String badgeUrl) {
            this.name = name;
            this.shortCode = shortCode;
            this.badgeUrl = badgeUrl;
        }
    }

    public SportMatch(
            String id,
            SportCategory category,
            String title,
            TeamInfo homeTeam,
            TeamInfo awayTeam,
            long kickoffTimestampMs,
            MatchStatus status,
            List<String> rawSources,
            Long elapsedSeconds
    ) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoffTimestampMs = kickoffTimestampMs;
        this.status = status;
        this.rawSources = rawSources;
        this.elapsedSeconds = elapsedSeconds;
    }
}
