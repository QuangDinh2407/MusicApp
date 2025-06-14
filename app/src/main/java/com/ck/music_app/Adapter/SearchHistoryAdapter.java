package com.ck.music_app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ck.music_app.Model.SearchHistory;
import com.ck.music_app.R;
import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.HistoryViewHolder> {

    private List<SearchHistory> historyList;
    private OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryClick(String query);

        void onHistoryRemove(String query);
    }

    public SearchHistoryAdapter(List<SearchHistory> historyList, OnHistoryClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SearchHistory history = historyList.get(position);
        holder.tvQuery.setText(history.getQuery());

        // Click vào item để tìm kiếm lại
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryClick(history.getQuery());
            }
        });

        // Click vào nút xóa
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryRemove(history.getQuery());
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void updateHistory(List<SearchHistory> newHistoryList) {
        this.historyList = newHistoryList;
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuery;
        ImageView btnRemove;

        HistoryViewHolder(View itemView) {
            super(itemView);
            tvQuery = itemView.findViewById(R.id.tvQuery);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}