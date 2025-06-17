package com.ck.music_app.Model;

import android.content.Context;
import android.net.Uri;

public class LocalSong {
    private String title;
    private String artist;
    private long duration;
    private Uri uri;
    private Context context;
    private String coverUrl;

    private String lyrics;

    public LocalSong() {}

    public LocalSong(String title, String artist, long duration, Uri uri, Context context, String coverUrl, String lyrics) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.uri = uri;
        this.context = context;
        this.coverUrl = coverUrl;
        this.lyrics = lyrics;
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }
}