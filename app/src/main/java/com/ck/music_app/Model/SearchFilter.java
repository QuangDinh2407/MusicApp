package com.ck.music_app.Model;

public enum SearchFilter {
    ALL("Tất cả"),
    SONGS("Bài hát"),
    ARTISTS("Nghệ sĩ"),
    ALBUMS("Album"),
    PLAYLISTS("Playlist");

    private final String displayName;

    SearchFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}