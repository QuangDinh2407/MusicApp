package com.ck.music_app.Model;

public class Album {
    private String id;
    private String title;
    private String coverUrl;
    private String description;
    private String releaseDate;
    private String artistId;

    public Album() {}

    public Album(String id, String title, String coverUrl, String description, String releaseDate, String artistId) {
        this.id = id;
        this.title = title;
        this.coverUrl = coverUrl;
        this.description = description;
        this.releaseDate = releaseDate;
        this.artistId = artistId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }
}