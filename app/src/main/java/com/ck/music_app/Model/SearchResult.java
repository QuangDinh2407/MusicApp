package com.ck.music_app.Model;

import java.io.Serializable;

public class SearchResult implements Serializable {
    public enum Type {
        SONG, ARTIST, ALBUM
    }

    private Type type;
    private Object data; // Song, Artist, hoặc Album
    private String title;
    private String subtitle;
    private String imageUrl;
    private String id;

    public SearchResult() {
    }

    public SearchResult(Type type, Object data, String title, String subtitle, String imageUrl, String id) {
        this.type = type;
        this.data = data;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.id = id;
    }

    // Static factory methods
    public static SearchResult fromSong(Song song) {
        return new SearchResult(
                Type.SONG,
                song,
                song.getTitle(),
                "Song • " + song.getArtistId(), // TODO: Get artist name
                song.getCoverUrl(),
                song.getSongId());
    }

    public static SearchResult fromArtist(Artist artist) {
        return new SearchResult(
                Type.ARTIST,
                artist,
                artist.getName(),
                "Artist • " + (artist.getFollowerCount() != null ? artist.getFollowerCount() + " followers" : ""),
                artist.getImageUrl(),
                artist.getArtistId());
    }

    public static SearchResult fromAlbum(Album album) {
        return new SearchResult(
                Type.ALBUM,
                album,
                album.getTitle(),
                "Album • " + album.getArtistId(), // TODO: Get artist name
                album.getCoverUrl(),
                album.getAlbumId());
    }

    // Getters and Setters
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Helper methods
    public Song getSong() {
        return type == Type.SONG ? (Song) data : null;
    }

    public Artist getArtist() {
        return type == Type.ARTIST ? (Artist) data : null;
    }

    public Album getAlbum() {
        return type == Type.ALBUM ? (Album) data : null;
    }
}