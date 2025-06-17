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

public class SearchAlbumAdapter extends RecyclerView.Adapter<SearchAlbumAdapter.AlbumViewHolder> {
    private Context context;
    private List<Album> albumList;
    private OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public SearchAlbumAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
    }

    public void setOnAlbumClickListener(OnAlbumClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albumList.get(position);

        holder.tvAlbumTitle.setText(album.getTitle());
        holder.tvAlbumArtist.setText(album.getArtistId() != null ? album.getArtistId() : "Various Artists");

        // Set album info text
        String albumInfo = "Album";
        holder.tvAlbumInfo.setText(albumInfo);

        // Load album cover
        Glide.with(context)
                .load(album.getCoverUrl())
                .placeholder(R.drawable.album)
                .error(R.drawable.album)
                .into(holder.imgAlbumCover);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlbumClick(album);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAlbumCover;
        TextView tvAlbumTitle, tvAlbumArtist, tvAlbumInfo;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAlbumCover = itemView.findViewById(R.id.imgAlbumCover);
            tvAlbumTitle = itemView.findViewById(R.id.tvAlbumTitle);
            tvAlbumArtist = itemView.findViewById(R.id.tvAlbumArtist);
            tvAlbumInfo = itemView.findViewById(R.id.tvAlbumInfo);
        }
    }
}