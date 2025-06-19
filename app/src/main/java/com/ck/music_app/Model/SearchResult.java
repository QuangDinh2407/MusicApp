package com.ck.music_app.Model;

import java.util.List;

public class SearchResult {
    private List<Song> songs;
    private List<Artist> artists;
    private List<Album> albums;
    private List<Playlist> playlists;
    private Song topSong;
    private Artist topArtist;

    public SearchResult() {
    }

    public SearchResult(List<Song> songs, List<Artist> artists, List<Album> albums, List<Playlist> playlists) {
        this.songs = songs;
        this.artists = artists;
        this.albums = albums;
        this.playlists = playlists;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(List<Album> albums) {
        this.albums = albums;
    }

    public List<Playlist> getPlaylists() {
        return playlists;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
    }

    public Song getTopSong() {
        return topSong;
    }

    public void setTopSong(Song topSong) {
        this.topSong = topSong;
    }

    public Artist getTopArtist() {
        return topArtist;
    }

    public void setTopArtist(Artist topArtist) {
        this.topArtist = topArtist;
    }

    public boolean hasResults() {
        return (songs != null && !songs.isEmpty()) ||
                (artists != null && !artists.isEmpty()) ||
                (albums != null && !albums.isEmpty()) ||
                (playlists != null && !playlists.isEmpty());
    }
}