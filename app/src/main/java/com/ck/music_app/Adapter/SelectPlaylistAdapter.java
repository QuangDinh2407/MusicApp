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

public class SelectPlaylistAdapter extends RecyclerView.Adapter<SelectPlaylistAdapter.ViewHolder> {
    private List<Playlist> playlists;
    private Context context;
    private OnPlaylistSelectedListener listener;

    public interface OnPlaylistSelectedListener {
        void onPlaylistSelected(Playlist playlist);
    }

    public SelectPlaylistAdapter(Context context, List<Playlist> playlists, OnPlaylistSelectedListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_select_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        
        holder.tvPlaylistName.setText(playlist.getName());
        holder.tvSongCount.setText(playlist.getSongIds().size() + " bài hát");

        // Load playlist cover
        Glide.with(context)
            .load(playlist.getCoverUrl())
            .placeholder(R.drawable.love)
            .error(R.drawable.love)
            .into(holder.imgPlaylistCover);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistSelected(playlist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlaylistCover;
        TextView tvPlaylistName;
        TextView tvSongCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlaylistCover = itemView.findViewById(R.id.imgPlaylistCover);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvSongCount = itemView.findViewById(R.id.tvSongCount);
        }
    }
} 