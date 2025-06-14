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

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder> {

    private List<String> suggestions;
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);

        void onInsertClick(String suggestion);
    }

    public SearchSuggestionAdapter(List<String> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        holder.tvSuggestion.setText(suggestion);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionClick(suggestion);
            }
        });

        holder.ivInsert.setOnClickListener(v -> {
            if (listener != null) {
                listener.onInsertClick(suggestion);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestions != null ? suggestions.size() : 0;
    }

    public void updateSuggestions(List<String> newSuggestions) {
        this.suggestions = newSuggestions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSuggestion;
        ImageView ivInsert;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSuggestion = itemView.findViewById(R.id.tvSuggestion);
            ivInsert = itemView.findViewById(R.id.ivInsert);
        }
    }
}