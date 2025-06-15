package com.ck.music_app.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.R;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private Context context;
    private List<Playlist> playlists;
    private OnPlaylistListener listener;
    private int longPressedPosition = -1; // To store position of long-pressed item

    public interface OnPlaylistListener {
        void onPlaylistClick(Playlist playlist);
        void onEditClick(Playlist playlist);
        void onDeleteClick(Playlist playlist);
        void onPlaylistLongClick(Playlist playlist, View view, int position); // New method for long click
    }

    public PlaylistAdapter(Context context, List<Playlist> playlists, OnPlaylistListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
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
        holder.tvName.setText(playlist.getName());
        int songCount = (playlist.getSongIds() != null) ? playlist.getSongIds().size() : 0;
        holder.tvSubtitle.setText(songCount + " bài hát");

        Glide.with(context).load(playlist.getCoverUrl()).into(holder.imgCover);
        
        // Click vào item để mở playlist
        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(playlist));

        // Click vào nút more để hiện popup menu
        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMore);
            popup.inflate(R.menu.playlist_context_menu);
            
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit_playlist) {
                    listener.onEditClick(playlist);
                    return true;
                } else if (id == R.id.action_delete_playlist) {
                    listener.onDeleteClick(playlist);
                    return true;
                }
                return false;
            });
            
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    public int getLongPressedPosition() {
        return longPressedPosition;
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvName;
        TextView tvSubtitle;
        ImageButton btnMore;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            tvName = itemView.findViewById(R.id.tvName);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
