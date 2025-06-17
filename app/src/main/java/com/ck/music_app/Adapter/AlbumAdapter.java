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
import com.ck.music_app.Model.Album;
import com.ck.music_app.R;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    private Context context;
    private List<Album> albums;
    private OnAlbumClickListener onAlbumClickListener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public AlbumAdapter(Context context, List<Album> albums) {
        this.context = context;
        this.albums = albums;
    }

    public void setOnAlbumClickListener(OnAlbumClickListener listener) {
        this.onAlbumClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albums.get(position);

        holder.tvAlbumTitle.setText(album.getTitle());

        // Load album cover
        Glide.with(context)
            .load(album.getCoverUrl())
            .placeholder(R.drawable.default_album_art)
            .error(R.drawable.default_album_art)
            .into(holder.imgAlbumCover);

        // Handle click events
        holder.itemView.setOnClickListener(v -> {
            if (onAlbumClickListener != null) {
                onAlbumClickListener.onAlbumClick(album);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAlbumCover;
        TextView tvAlbumTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAlbumCover = itemView.findViewById(R.id.imgAlbumCover);
            tvAlbumTitle = itemView.findViewById(R.id.tvAlbumTitle);
        }
    }

    public void updateAlbums(List<Album> newAlbums) {
        this.albums = newAlbums;
        notifyDataSetChanged();
    }
} 