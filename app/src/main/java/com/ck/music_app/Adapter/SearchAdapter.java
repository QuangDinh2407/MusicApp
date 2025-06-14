package com.ck.music_app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.Model.Song;
import com.ck.music_app.R;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
    private List<Song> songs;
    private final OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SearchAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvTitle.setText(song.getTitle());

        // Hiển thị tên nghệ sĩ thay vì view count
        // TODO: Lấy tên nghệ sĩ từ Firestore dựa trên artistId
        holder.tvArtist.setText("DJ " + song.getArtistId());

        // Load ảnh bìa
        // TODO: Load image using Glide

        // Xử lý click vào item
        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));

        // Xử lý click vào nút play
        holder.btnPlay.setOnClickListener(v -> listener.onSongClick(song));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void updateSongs(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        ImageView btnPlay;
        TextView tvTitle;
        TextView tvArtist;

        SearchViewHolder(View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnPlay = itemView.findViewById(R.id.btnPlay);
        }
    }
}