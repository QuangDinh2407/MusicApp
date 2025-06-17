package com.ck.music_app.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;

import java.util.List;

public class PlaylistSongAdapter extends RecyclerView.Adapter<PlaylistSongAdapter.SongViewHolder> {
    private Context context;
    private List<Song> songs;
    private OnSongActionListener listener;

    public interface OnSongActionListener {
        void onSongClick(Song song);
        void onRemoveClick(Song song, int position);
        void onMoreClick(Song song, View view);
    }

    public PlaylistSongAdapter(Context context, List<Song> songs, OnSongActionListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        
        holder.tvTitle.setText(song.getTitle());
        
        // Load song cover
        Glide.with(context)
            .load(song.getCoverUrl())
            .placeholder(R.drawable.love)
            .error(R.drawable.love)
            .into(holder.imgCover);

        // Click listeners
        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
        holder.btnMore.setOnClickListener(v -> listener.onMoreClick(song, v));
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void updateSongs(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    public void removeSong(int position) {
        songs.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, songs.size());
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle;
        ImageButton btnMore;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
} 