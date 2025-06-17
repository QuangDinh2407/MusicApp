package com.ck.music_app.Model;

public class TrendingSong {
    private String title;
    private String artist;
    private int rank;

    public TrendingSong() {}

    public TrendingSong(String title, String artist, int rank) {
        this.title = title;
        this.artist = artist;
        this.rank = rank;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
} 