package com.ck.music_app.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Song implements Serializable {
    private String songId;
    private List<String> albumIds;
    private String artistId;
    private String audioUrl;
    private String coverUrl;
    private String createAt;
    private Integer duration;
    private String genreId;
    private Integer likeCount;
    private String title;
    private Integer viewCount;

    public Song() {
        this.albumIds = new ArrayList<>();
    }

    public Song(String songId, String albumId, String artistId, String audioUrl, String coverUrl, String createAt,
            Integer duration, String genreId, Integer likeCount, String title, Integer viewCount) {
        this.songId = songId;
        this.albumIds = new ArrayList<>();
        if (albumId != null) {
            this.albumIds.add(albumId);
        }
        this.artistId = artistId;
        this.audioUrl = audioUrl;
        this.coverUrl = coverUrl;
        this.createAt = createAt;
        this.duration = duration;
        this.genreId = genreId;
        this.likeCount = likeCount;
        this.title = title;
        this.viewCount = viewCount;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }

    public List<String> getAlbumIds() {
        return albumIds;
    }

    public void setAlbumIds(List<String> albumIds) {
        this.albumIds = albumIds != null ? albumIds : new ArrayList<>();
    }

    public String getAlbumId() {
        return albumIds != null && !albumIds.isEmpty() ? albumIds.get(0) : null;
    }

    public void setAlbumId(String albumId) {
        if (this.albumIds == null) {
            this.albumIds = new ArrayList<>();
        }
        if (!this.albumIds.isEmpty()) {
            this.albumIds.clear();
        }
        if (albumId != null) {
            this.albumIds.add(albumId);
        }
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    // Method to check if song matches search query
    public boolean matchesSearch(String query) {
        if (query == null || query.isEmpty())
            return false;
        query = query.toLowerCase();
        return title.toLowerCase().contains(query) ||
                artistId.toLowerCase().contains(query); // Tìm theo artistId
    }
}
