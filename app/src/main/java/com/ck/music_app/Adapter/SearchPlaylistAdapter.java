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

public class SearchPlaylistAdapter extends RecyclerView.Adapter<SearchPlaylistAdapter.PlaylistViewHolder> {
    private Context context;
    private List<Playlist> playlistList;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public SearchPlaylistAdapter(Context context, List<Playlist> playlistList) {
        this.context = context;
        this.playlistList = playlistList;
    }

    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);

        holder.tvPlaylistName.setText(playlist.getName());
        holder.tvPlaylistCreator.setText(playlist.getUserId() != null ? playlist.getUserId() : "Unknown");

        // Set playlist info text
        String playlistInfo = "Playlist";
        if (playlist.getSongIds() != null && !playlist.getSongIds().isEmpty()) {
            playlistInfo += " • " + playlist.getSongIds().size() + " bài hát";
        }
        holder.tvPlaylistInfo.setText(playlistInfo);

        // Load playlist cover (or use default)
        String coverUrl = playlist.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            // Use default playlist icon
            holder.imgPlaylistCover.setImageResource(R.drawable.playlist_default);
        } else {
            Glide.with(context)
                    .load(coverUrl)
                    .placeholder(R.drawable.playlist_default)
                    .error(R.drawable.playlist_default)
                    .into(holder.imgPlaylistCover);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlaylistCover;
        TextView tvPlaylistName, tvPlaylistCreator, tvPlaylistInfo;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlaylistCover = itemView.findViewById(R.id.imgPlaylistCover);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvPlaylistCreator = itemView.findViewById(R.id.tvPlaylistCreator);
            tvPlaylistInfo = itemView.findViewById(R.id.tvPlaylistInfo);
        }
    }
}