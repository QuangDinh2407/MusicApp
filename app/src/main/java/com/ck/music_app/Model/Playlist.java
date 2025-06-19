package com.ck.music_app.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable {
    private String id;
    private String name;
    private String coverUrl;
    private String createdAt;
    private String lastAccessedAt;
    private List<String> songIds;

    private String userId;

    public Playlist() {
        // Empty constructor needed for Firestore
        songIds = new ArrayList<>();
    }

    public Playlist(String id, String name, List<String> songIds, String coverUrl, String createdAt) {
        this.id = id;
        this.name = name;
        this.songIds = songIds != null ? songIds : new ArrayList<>();
        this.coverUrl = coverUrl;
        this.createdAt = createdAt;
        this.lastAccessedAt = createdAt;
    }

    public Playlist(String id, String name, String coverUrl, String createdAt, String lastAccessedAt, List<String> songIds, String userId) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
        this.songIds = songIds;
        this.userId = userId;
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

    public List<String> getSongIds() {
        if (songIds == null) {
            songIds = new ArrayList<>();
        }
        return songIds;
    }

    public void setSongIds(List<String> songIds) {
        this.songIds = songIds != null ? songIds : new ArrayList<>();
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(String lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
