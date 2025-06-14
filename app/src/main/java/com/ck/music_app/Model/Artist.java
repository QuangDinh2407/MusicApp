package com.ck.music_app.Model;

import java.io.Serializable;

public class Artist implements Serializable {
    private String artistId;
    private String name;
    private String bio;
    private String imageUrl;
    private Integer followerCount;
    private String genreId;
    private String createAt;

    public Artist() {
    }

    public Artist(String artistId, String name, String bio, String imageUrl,
            Integer followerCount, String genreId, String createAt) {
        this.artistId = artistId;
        this.name = name;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.followerCount = followerCount;
        this.genreId = genreId;
        this.createAt = createAt;
    }

    // Getters and Setters
    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }
}