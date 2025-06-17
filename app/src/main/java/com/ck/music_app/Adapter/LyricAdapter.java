package com.ck.music_app.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.Model.LyricLine;
import com.ck.music_app.R;

import java.util.ArrayList;
import java.util.List;

public class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.LyricViewHolder> {
    private List<LyricLine> lyricLines;
    private Context context;
    private static final String TAG = "LyricAdapter";

    public LyricAdapter(Context context) {
        this.context = context;
        this.lyricLines = new ArrayList<>();
    }

    @NonNull
    @Override
    public LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_lyric, parent, false);
        return new LyricViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricViewHolder holder, int position) {
        LyricLine line = lyricLines.get(position);
        holder.tvLyric.setText(line.getText());
        
        if (line.isHighlighted()) {
            holder.tvLyric.setTextColor(Color.YELLOW);
        } else {
            holder.tvLyric.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return lyricLines.size();
    }

    public void setLyrics(List<LyricLine> lyrics) {
        
        this.lyricLines = lyrics;
        notifyDataSetChanged();
    }

    public void updateHighlight(int currentTimeMs) {

        
        // Reset tất cả highlight
        for (LyricLine line : lyricLines) {
            line.setHighlighted(false);
        }

        if (lyricLines.isEmpty()) {
            return;
        }

        // Tìm dòng lyrics phù hợp với thời gian hiện tại
        int currentLineIndex = -1;
        
        // Duyệt qua từng dòng lyrics
        for (int i = 0; i < lyricLines.size(); i++) {
            LyricLine currentLine = lyricLines.get(i);
            long currentLineTime = currentLine.getTimeMs();
            
            // Nếu là dòng cuối
            if (i == lyricLines.size() - 1) {
                if (currentTimeMs >= currentLineTime) {
                    currentLineIndex = i;

                }
                break;
            }
            
            // Lấy thời gian của dòng tiếp theo
            long nextLineTime = lyricLines.get(i + 1).getTimeMs();
            
            // Nếu thời gian hiện tại nằm trong khoảng của dòng hiện tại
            if (currentTimeMs >= currentLineTime && currentTimeMs < nextLineTime) {
                currentLineIndex = i;

                break;
            }
            
            // Nếu thời gian hiện tại lớn hơn dòng hiện tại nhưng nhỏ hơn dòng tiếp theo
            if (i > 0 && currentTimeMs >= lyricLines.get(i-1).getTimeMs() && currentTimeMs < currentLineTime) {
                currentLineIndex = i-1;

                break;
            }
        }

        // Highlight dòng tìm được
        if (currentLineIndex != -1) {
            lyricLines.get(currentLineIndex).setHighlighted(true);
            notifyDataSetChanged();
        }

    }

    public boolean isHighlighted(int position) {
        if (position >= 0 && position < lyricLines.size()) {
            return lyricLines.get(position).isHighlighted();
        }
        return false;
    }

    static class LyricViewHolder extends RecyclerView.ViewHolder {
        TextView tvLyric;

        public LyricViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLyric = itemView.findViewById(R.id.tvLyric);
        }
    }
} 