package com.ck.music_app.interfaces;

import com.ck.music_app.Model.Song;
import java.util.List;

public interface OnSongClickListener {
    void onSongClick(List<Song> songList, int position);
} 