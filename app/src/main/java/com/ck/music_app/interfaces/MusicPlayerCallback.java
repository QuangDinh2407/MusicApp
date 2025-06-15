package com.ck.music_app.interfaces;

public interface MusicPlayerCallback {
    void onPlaybackStateChanged(boolean isPlaying);
    void onSongChanged(int position);
    void onProgressChanged(int progress, int duration);
} 