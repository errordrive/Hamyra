package com.example.data.model;

/**
 * Reconstructed Subtitle Track model.
 */
public final class Subtitle {
    public final String url;
    public final String label;
    public final String language;
    public final boolean isDefault;

    public Subtitle(String url, String label, String language, boolean isDefault) {
        this.url = url;
        this.label = label;
        this.language = language;
        this.isDefault = isDefault;
    }
}
