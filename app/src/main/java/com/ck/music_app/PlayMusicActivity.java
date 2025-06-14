package com.ck.music_app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.ck.music_app.Service.MediaPlayerService;

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

public class PlayMusicActivity extends AppCompatActivity implements MediaPlayerService.MediaPlayerCallback {

    private ImageView imgVinyl, imgCover;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnBack, btnPrevious, btnNext;

    private List<Song> songList = new ArrayList<>();

    private int currentIndex;
    private Handler handler = new Handler();
    private ObjectAnimator vinylRotationAnimator;
    private ObjectAnimator coverRotationAnimator;

    private float currentRotating = 0f;
    private boolean isVinylRotating = false;
    private boolean isCoverRotating = false;

    private MediaPlayerService mediaPlayerService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            serviceBound = true;

            // Set callback để nhận thông báo từ service
            mediaPlayerService.setCallback(PlayMusicActivity.this);

            // Set playlist và phát nhạc khi service đã kết nối
            mediaPlayerService.setPlaylist(songList);
            mediaPlayerService.playSong(currentIndex);

            // Cập nhật UI ban đầu
            updateUI();
            // Bắt đầu cập nhật seekbar
            handler.postDelayed(updateSeekBar, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (mediaPlayerService != null) {
                mediaPlayerService.removeCallback();
            }
            mediaPlayerService = null;
            serviceBound = false;
        }
    };

    // Implement callback methods từ MediaPlayerService
    @Override
    public void onSongChanged(Song song, int position) {
        runOnUiThread(() -> {
            currentIndex = position;
            updateSongInfo(song);
        });
    }

    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            updatePlayPauseButton(isPlaying);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);
        songList = (List<Song>) getIntent().getSerializableExtra("songList");
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        // Khởi động service
        Intent serviceIntent = new Intent(this, MediaPlayerService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        initViews();
        initListeners();
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
            if (serviceBound) {
                mediaPlayerService.togglePlayPause();
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnPrevious.setOnClickListener(v -> {
            if (serviceBound) {
                mediaPlayerService.playPrevious();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (serviceBound) {
                mediaPlayerService.playNext();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    mediaPlayerService.seekTo(progress);
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

    private void updateUI() {
        if (serviceBound) {
            Song currentSong = mediaPlayerService.getCurrentSong();
            if (currentSong != null) {
                updateSongInfo(currentSong);
            }
            updatePlayPauseButton(mediaPlayerService.isPlaying());
        }
    }

    private void updateSongInfo(Song song) {
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtistId());
        Glide.with(this)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .circleCrop()
                .into(imgCover);

        // Update gradient
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
                    public void onLoadCleared(Drawable placeholder) {
                    }
                });
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        btnPlayPause.setImageResource(
                isPlaying ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_white_36dp);
        if (isPlaying) {
            startVinylRotation();
            startCoverRotation();
        } else {
            stopVinylRotation();
            stopCoverRotation();
        }
    }

    private void startVinylRotation() {
        isVinylRotating = true;
        imgVinyl.setRotation(currentRotating);
        imgVinyl.animate().rotationBy(360f + currentRotating).setDuration(8000)
                .setInterpolator(new LinearInterpolator()).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (isVinylRotating)
                            startVinylRotation();
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
        imgCover.animate().rotationBy(360f + currentRotating).setDuration(8000)
                .setInterpolator(new LinearInterpolator()).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (isCoverRotating)
                            startCoverRotation();
                    }
                }).start();
    }

    private void stopCoverRotation() {
        isCoverRotating = false;
        imgCover.animate().cancel();
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (serviceBound && mediaPlayerService != null) {
                int currentPosition = mediaPlayerService.getCurrentPosition();
                int duration = mediaPlayerService.getDuration();
                seekBar.setMax(duration);
                seekBar.setProgress(currentPosition);
                tvCurrentTime.setText(MusicUtils.formatTime(currentPosition));
                tvTotalTime.setText(MusicUtils.formatTime(duration));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            if (mediaPlayerService != null) {
                mediaPlayerService.removeCallback();
            }
            unbindService(serviceConnection);
            serviceBound = false;
        }
        handler.removeCallbacks(updateSeekBar);
        stopVinylRotation();
        stopCoverRotation();
    }
}