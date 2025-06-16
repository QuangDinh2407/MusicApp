package com.ck.music_app.Model;

public class LyricLine {
    private long timeMs; // Time in milliseconds
    private String text;
    private boolean isHighlighted;

    public LyricLine(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text;
        this.isHighlighted = false;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public String getText() {
        return text;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }
} 