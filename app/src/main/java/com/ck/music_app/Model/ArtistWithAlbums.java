package com.ck.music_app.Model;

import java.util.List;

public class ArtistWithAlbums {
    private Artist artist;
    private List<Album> albums;


    public ArtistWithAlbums() {}

    public ArtistWithAlbums(Artist artist, List<Album> albums) {
        this.artist = artist;
        this.albums = albums;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(List<Album> albums) {
        this.albums = albums;
    }
}