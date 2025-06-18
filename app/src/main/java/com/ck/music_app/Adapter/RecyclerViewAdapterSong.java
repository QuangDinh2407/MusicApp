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
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.interfaces.OnSongClickListener;

import java.util.List;

public class RecyclerViewAdapterSong extends RecyclerView.Adapter<RecyclerViewAdapterSong.ViewHolder> {
    private List<Song> songList;
    private Context context;
    private OnSongClickListener onSongClickListener;

    public RecyclerViewAdapterSong(Context context, List<Song> songList) {
        this.context = context;
        this.songList = songList;
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.onSongClickListener = listener;
    }

    public void updateSongList(List<Song> newSongList) {
        this.songList = newSongList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvViewCount.setText("Lượt nghe: " + song.getViewCount());

        Glide.with(context)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            if (onSongClickListener != null) {
                onSongClickListener.onSongClick(songList, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvArtist, tvViewCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvViewCount = itemView.findViewById(R.id.tvViewCount);
        }
    }
}