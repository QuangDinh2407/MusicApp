package com.ck.music_app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;

public class MiniPlayerFragment extends Fragment {
    private ImageView imgSongCover;
    private TextView tvSongTitle;
    private TextView tvArtistName;
    private ImageButton btnPlayPause;
    private Song currentSong;
    private boolean isPlaying = false;

    private OnMiniPlayerClickListener listener;

    public interface OnMiniPlayerClickListener {
        void onMiniPlayerClicked();
        void onPlayPauseClicked();
    }

    public void setOnMiniPlayerClickListener(OnMiniPlayerClickListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mini_player, container, false);

        imgSongCover = view.findViewById(R.id.imgSongCover);
        tvSongTitle = view.findViewById(R.id.tvSongTitle);
        tvArtistName = view.findViewById(R.id.tvArtistName);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);

        // Click vào mini player để mở full player
        view.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMiniPlayerClicked();
            }
        });

        // Click vào nút play/pause
        btnPlayPause.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayPauseClicked();
            }
        });

        return view;
    }

    public void updateSong(Song song) {
        this.currentSong = song;
        if (song != null && isAdded()) {
            tvSongTitle.setText(song.getTitle());
            tvArtistName.setText(song.getArtistId());
            Glide.with(requireContext())
                    .load(song.getCoverUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(imgSongCover);
        }
    }

    public void updatePlayingState(boolean isPlaying) {
        this.isPlaying = isPlaying;
        if (isAdded()) {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_white_36dp);
        }
    }
} 