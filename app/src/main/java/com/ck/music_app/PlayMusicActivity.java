package com.ck.music_app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ck.music_app.Model.Song;
import com.ck.music_app.utils.MusicUtils;
import com.ck.music_app.utils.GradientUtils;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class PlayMusicActivity extends AppCompatActivity {

    private ImageView imgVinyl, imgCover;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnBack, btnPrevious, btnNext;

    private List<Song> songList = new ArrayList<>();

    private int currentIndex;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private Handler handler = new Handler();
    private ObjectAnimator vinylRotationAnimator;
    private ObjectAnimator coverRotationAnimator;

    private float currentRotating = 0f;
    private boolean isVinylRotating = false;
    private boolean isCoverRotating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);
        songList = (List<Song>) getIntent().getSerializableExtra("songList");
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        initViews();
        initListeners();
        loadSong(currentIndex);

    }

    private void initViews() {
        imgCover = findViewById(R.id.imgCover);
        imgVinyl = findViewById(R.id.imgVinyl);
        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnBack = findViewById(R.id.btnBack);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
    }

    private void initListeners() {
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseMusic();
            } else {
                playMusic();
            }
        });
        btnBack.setOnClickListener(v -> finish());
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
        // Khởi tạo lại mediaPlayer với audioUrl mới
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
        // Gradient động
        View gradientOverlay = findViewById(R.id.gradientOverlay);
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
        // Khởi tạo lại ObjectAnimator cho xoay đĩa nhạc và cover
        if (vinylRotationAnimator != null) vinylRotationAnimator.cancel();
        if (coverRotationAnimator != null) coverRotationAnimator.cancel();

    }

    private void startVinylRotation() {
        isVinylRotating = true;
        imgVinyl.setRotation(currentRotating);
        imgVinyl.animate().rotationBy(360f + currentRotating).setDuration(8000).setInterpolator(new LinearInterpolator()).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (isVinylRotating) startVinylRotation();
            }
        }).start();
    }

    private void stopVinylRotation() {
        isVinylRotating = false;
        imgVinyl.animate().cancel();
        currentRotating = imgVinyl.getRotation();
    }

    private void startCoverRotation() {
        isCoverRotating = true;
        imgCover.setRotation(currentRotating);
        imgCover.animate().rotationBy(360f + currentRotating).setDuration(8000).setInterpolator(new LinearInterpolator()).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (isCoverRotating) startCoverRotation();
            }
        }).start();
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

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                tvCurrentTime.setText(MusicUtils.formatTime(currentPosition));
                handler.postDelayed(this, 0);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBar);
        stopVinylRotation();
        stopCoverRotation();
    }
} 