package com.ck.music_app.Model;

import java.io.Serializable;

public class Album implements Serializable {
    private String albumId;
    private String title;
    private String artistId;
    private String coverUrl;
    private String releaseDate;
    private String genreId;
    private Integer trackCount;
    private String createAt;

    public Album() {
    }

    public Album(String albumId, String title, String artistId, String coverUrl,
            String releaseDate, String genreId, Integer trackCount, String createAt) {
        this.albumId = albumId;
        this.title = title;
        this.artistId = artistId;
        this.coverUrl = coverUrl;
        this.releaseDate = releaseDate;
        this.genreId = genreId;
        this.trackCount = trackCount;
        this.createAt = createAt;
    }

    // Getters and Setters
    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }

    public Integer getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(Integer trackCount) {
        this.trackCount = trackCount;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }
}