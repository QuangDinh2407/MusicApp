package com.ck.music_app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.SearchResult;
import com.ck.music_app.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<SearchResult> results;
    private OnResultClickListener listener;

    public interface OnResultClickListener {
        void onResultClick(SearchResult result);

        void onActionClick(SearchResult result);
    }

    public SearchResultAdapter(List<SearchResult> results, OnResultClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);

        holder.tvTitle.setText(result.getTitle());
        holder.tvSubtitle.setText(result.getSubtitle());

        // Load image
        if (result.getImageUrl() != null && !result.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(result.getImageUrl())
                    .placeholder(R.drawable.rounded_album_cover)
                    .error(R.drawable.rounded_album_cover)
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.rounded_album_cover);
        }

        // Set type icon
        switch (result.getType()) {
            case SONG:
                holder.ivTypeIcon.setImageResource(R.drawable.ic_music_note);
                holder.ivAction.setImageResource(R.drawable.ic_play_circle);
                break;
            case ARTIST:
                holder.ivTypeIcon.setImageResource(R.drawable.ic_person);
                holder.ivAction.setImageResource(R.drawable.ic_arrow_forward);
                break;
            case ALBUM:
                holder.ivTypeIcon.setImageResource(R.drawable.ic_album);
                holder.ivAction.setImageResource(R.drawable.ic_arrow_forward);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResultClick(result);
            }
        });

        holder.ivAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionClick(result);
            }
        });
    }

    @Override
    public int getItemCount() {
        return results != null ? results.size() : 0;
    }

    public void updateResults(List<SearchResult> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivCover;
        TextView tvTitle;
        TextView tvSubtitle;
        ImageView ivTypeIcon;
        ImageView ivAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
            ivAction = itemView.findViewById(R.id.ivAction);
        }
    }
}