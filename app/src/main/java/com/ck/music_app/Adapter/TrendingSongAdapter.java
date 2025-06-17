package com.ck.music_app.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.Model.TrendingSong;
import com.ck.music_app.R;

import java.util.List;

public class TrendingSongAdapter extends RecyclerView.Adapter<TrendingSongAdapter.ViewHolder> {
    private List<TrendingSong> trendingSongs;
    private Context context;
    private OnTrendingSongClickListener onClickListener;

    public interface OnTrendingSongClickListener {
        void onTrendingSongClick(TrendingSong song, int position);
    }

    public TrendingSongAdapter(Context context, List<TrendingSong> trendingSongs) {
        this.context = context;
        this.trendingSongs = trendingSongs;
    }

    public void setOnTrendingSongClickListener(OnTrendingSongClickListener listener) {
        this.onClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trending_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrendingSong song = trendingSongs.get(position);
        Log.d("TrendingSongAdapter", "Binding item " + position + ": " + song.getTitle());
        holder.tvRank.setText(String.valueOf(song.getRank()));
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onClickListener != null) {
                onClickListener.onTrendingSongClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d("TrendingSongAdapter", "getItemCount: " + trendingSongs.size());
        return trendingSongs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvTitle, tvArtist;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
        }
    }
} 