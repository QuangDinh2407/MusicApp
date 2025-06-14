package com.ck.music_app.Model;

import java.io.Serializable;

public class SearchHistory implements Serializable {
    private String query;
    private long timestamp;
    private String type; // "song", "artist", "query"

    public SearchHistory() {
    }

    public SearchHistory(String query, long timestamp, String type) {
        this.query = query;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}