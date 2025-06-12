package com.ck.music_app.Model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Song implements Serializable {
    private String songId;

    private List<String> albumId;

    private String artistId;

    private String audioUrl;

    private String coverUrl;

    private String createAt;

    private Integer duration;

    private String genreId;

    private Integer likeCount;

    private String title;

    private Integer viewCount;

    public Song() {}

    public Song(String songId, List<String> albumId, String artistId, String audioUrl, String coverUrl, String createAt, Integer duration, String genreId, Integer likeCount, String title, Integer viewCount) {
        this.songId = songId;
        this.albumId = albumId;
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

    public List<String> getAlbumId() {
        return albumId;
    }

    public void setAlbumId(List<String> albumId) {
        this.albumId = albumId;
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
}
