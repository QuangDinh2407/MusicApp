package com.ck.music_app.Model;

import java.io.Serializable;
import java.util.List;

public class Playlist implements Serializable {
    private String id;
    private String name;
    private String description;
    private String coverUrl;
    private String userId;
    private String createdAt;
    private List<String> songIds;
    private boolean isPublic;

    public Playlist() {
    }

    public Playlist(String id, String name, String description, String coverUrl, String userId, String createdAt,
            List<String> songIds, boolean isPublic) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.coverUrl = coverUrl;
        this.userId = userId;
        this.createdAt = createdAt;
        this.songIds = songIds;
        this.isPublic = isPublic;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getSongIds() {
        return songIds;
    }

    public void setSongIds(List<String> songIds) {
        this.songIds = songIds;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
}