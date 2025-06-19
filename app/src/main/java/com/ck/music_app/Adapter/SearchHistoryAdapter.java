package com.ck.music_app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.R;

import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private List<String> searchHistory;
    private OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryClick(String query);

        void onHistoryDelete(String query);
    }

    public SearchHistoryAdapter(List<String> searchHistory) {
        this.searchHistory = searchHistory;
    }

    public void setOnHistoryClickListener(OnHistoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = searchHistory.get(position);
        holder.tvQuery.setText(query);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryClick(query);
            }
        });

        holder.imgDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryDelete(query);
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchHistory.size();
    }

    public void updateHistory(List<String> newHistory) {
        this.searchHistory = newHistory;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuery;
        ImageView imgDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuery = itemView.findViewById(R.id.tvQuery);
            imgDelete = itemView.findViewById(R.id.imgDelete);
        }
    }
}