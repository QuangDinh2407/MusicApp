package com.ck.music_app.Model;

import java.io.Serializable;
import java.util.List;

public class Playlist implements Serializable {
    private String id;
    private String name;
    private String coverUrl;
    private List<String> songIds;
    private String createdAt;

    public Playlist() {}

    public Playlist(String id, String name, List<String> songIds, String coverUrl, String createdAt) {
        this.id = id;
        this.name = name;
        this.songIds = songIds;
        this.coverUrl = coverUrl;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public List<String> getSongIds() {
        return songIds;
    }

    public void setSongIds(List<String> songIds) {
        this.songIds = songIds;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
