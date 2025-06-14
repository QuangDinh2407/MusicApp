package com.ck.music_app.Fragment;

import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.utils.GradientUtils;
import com.ck.music_app.utils.MusicUtils;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

public class PlayMusicFragment extends Fragment {

    private ImageView imgVinyl, imgCover;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnBack, btnPrevious, btnNext;
    private View gradientOverlay;

    private List<Song> songList = new ArrayList<>();
    private int currentIndex;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private Handler handler = new Handler();
    private float currentRotating = 0f;
    private boolean isVinylRotating = false;
    private boolean isCoverRotating = false;

    public static PlayMusicFragment newInstance(List<Song> songs, int currentIndex) {
        PlayMusicFragment fragment = new PlayMusicFragment();
        Bundle args = new Bundle();
        args.putSerializable("songList", new ArrayList<>(songs));
        args.putInt("currentIndex", currentIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songList = (List<Song>) getArguments().getSerializable("songList");
            currentIndex = getArguments().getInt("currentIndex", 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play_music, container, false);
        initViews(view);
        initListeners();
        loadSong(currentIndex);
        return view;
    }

    private void initViews(View view) {
        imgCover = view.findViewById(R.id.imgCover);
        imgVinyl = view.findViewById(R.id.imgVinyl);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvArtist = view.findViewById(R.id.tvArtist);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        seekBar = view.findViewById(R.id.seekBar);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnBack = view.findViewById(R.id.btnBack);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        gradientOverlay = view.findViewById(R.id.gradientOverlay);
    }

    private void initListeners() {
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseMusic();
            } else {
                playMusic();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).minimizePlayer();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                loadSong(currentIndex);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < songList.size() - 1) {
                currentIndex++;
                loadSong(currentIndex);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(MusicUtils.formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.postDelayed(updateSeekBar, 1000);
            }
        });
    }

    private void loadSong(int index) {
        Song song = songList.get(index);
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtistId());
        
        Glide.with(this)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .circleCrop()
                .into(imgCover);

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(song.getAudioUrl());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mediaPlayer.getDuration());
                tvTotalTime.setText(MusicUtils.formatTime(mediaPlayer.getDuration()));
                playMusic();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        Glide.with(this)
            .asBitmap()
            .load(song.getCoverUrl())
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    GradientUtils.createGradientFromBitmap(resource, gradientOverlay);
                }

                @Override
                public void onLoadCleared(Drawable placeholder) {}
            });

        if (isVinylRotating) stopVinylRotation();
        if (isCoverRotating) stopCoverRotation();
    }

    private void startVinylRotation() {
        isVinylRotating = true;
        imgVinyl.setRotation(currentRotating);
        imgVinyl.animate()
                .rotationBy(360f + currentRotating)
                .setDuration(8000)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> {
                    if (isVinylRotating) startVinylRotation();
                })
                .start();
    }

    private void stopVinylRotation() {
        isVinylRotating = false;
        imgVinyl.animate().cancel();
        currentRotating = imgVinyl.getRotation();
    }

    private void startCoverRotation() {
        isCoverRotating = true;
        imgCover.setRotation(currentRotating);
        imgCover.animate()
                .rotationBy(360f + currentRotating)
                .setDuration(8000)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> {
                    if (isCoverRotating) startCoverRotation();
                })
                .start();
    }

    private void stopCoverRotation() {
        isCoverRotating = false;
        imgCover.animate().cancel();
    }

    private void playMusic() {
        mediaPlayer.start();
        btnPlayPause.setImageResource(R.drawable.ic_pause_white_36dp);
        isPlaying = true;
        startVinylRotation();
        startCoverRotation();
        handler.postDelayed(updateSeekBar, 1000);
    }

    private void pauseMusic() {
        mediaPlayer.pause();
        btnPlayPause.setImageResource(R.drawable.ic_play_white_36dp);
        isPlaying = false;
        stopVinylRotation();
        stopCoverRotation();
        handler.removeCallbacks(updateSeekBar);
    }

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                tvCurrentTime.setText(MusicUtils.formatTime(currentPosition));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBar);
    }

    public void updatePlayingState(boolean isPlaying) {
        if (this.isPlaying != isPlaying) {
            this.isPlaying = isPlaying;
            if (isPlaying) {
                playMusic();
            } else {
                pauseMusic();
            }
        }
    }
} 