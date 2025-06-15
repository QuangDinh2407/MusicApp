package com.ck.music_app.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ck.music_app.interfaces.OnSongClickListener;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;

import java.util.List;

public class AlbumSongsAdapter extends ArrayAdapter<Song> {
    private Context context;
    private List<Song> songs;
    private OnSongClickListener onSongClickListener;

    public AlbumSongsAdapter(Context context, List<Song> songs) {
        super(context, 0, songs);
        this.context = context;
        this.songs = songs;
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.onSongClickListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        }

        Song song = getItem(position);

        ImageView imgCover = convertView.findViewById(R.id.imgCover);
        TextView tvTitle = convertView.findViewById(R.id.tvTitle);

        tvTitle.setText(song.getTitle());

        Glide.with(context)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imgCover);

        convertView.setOnClickListener(v -> {
            if (onSongClickListener != null) {
                onSongClickListener.onSongClick(songs, position);
            }
        });

        return convertView;
    }
} 