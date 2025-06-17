package com.ck.music_app.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.R;

import java.util.List;

public class PlaylistRecyclerAdapter extends RecyclerView.Adapter<PlaylistRecyclerAdapter.PlaylistViewHolder> {
    private Context context;
    private List<Playlist> playlists;
    private OnPlaylistListener onPlaylistListener;
    private String selectedPlaylistId = null;

    public interface OnPlaylistListener {
        void onPlaylistClick(Playlist playlist);
        void onPlaylistLongClick(Playlist playlist, View view, int position);
    }

    public PlaylistRecyclerAdapter(Context context, List<Playlist> playlists, OnPlaylistListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.onPlaylistListener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);

        holder.tvPlaylistName.setText(playlist.getName());
        holder.tvSongCount.setText(playlist.getSongIds().size() + " bài hát");

        // Load playlist cover image
        Glide.with(context)
            .load(playlist.getCoverUrl())
            .placeholder(R.drawable.love)
            .error(R.drawable.love)
            .into(holder.imgPlaylist);

        // Highlight selected playlist
        if (selectedPlaylistId != null && selectedPlaylistId.equals(playlist.getId())) {
            holder.itemView.setBackgroundResource(R.color.selected_playlist_background);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (onPlaylistListener != null) {
                onPlaylistListener.onPlaylistClick(playlist);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onPlaylistListener != null) {
                onPlaylistListener.onPlaylistLongClick(playlist, v, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void setPlaylists(List<Playlist> newPlaylists) {
        this.playlists = newPlaylists;
        notifyDataSetChanged();
    }

    public void setSelectedPlaylist(String playlistId) {
        this.selectedPlaylistId = playlistId;
        notifyDataSetChanged();
    }

    public String getSelectedPlaylistId() {
        return selectedPlaylistId;
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlaylist;
        TextView tvPlaylistName;
        TextView tvSongCount;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlaylist = itemView.findViewById(R.id.imgPlaylist);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvSongCount = itemView.findViewById(R.id.tvSongCount);
        }
    }
} 