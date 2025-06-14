package com.ck.music_app;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.Services.FirebaseService;
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
    private MusicService musicService;
    private FirebaseService firebaseService;
    private boolean serviceBound = false;
    private Handler handler = new Handler();
    private ObjectAnimator vinylRotationAnimator;
    private ObjectAnimator coverRotationAnimator;

    private float currentRotating = 0f;
    private boolean isVinylRotating = false;
    private boolean isCoverRotating = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            musicService.setList(songList, currentIndex);
            loadSong(currentIndex);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);
        songList = (List<Song>) getIntent().getSerializableExtra("songList");
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        firebaseService = FirebaseService.getInstance();
        
        initViews();
        initListeners();
        
        // Bind to MusicService
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
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
            if (musicService.isPlaying()) {
                pauseMusic();
            } else {
                playMusic();
            }
        });

        btnBack.setOnClickListener(v -> {
            // Chỉ finish() activity, không dừng service
            finish();
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                musicService.playPrevious();
                updateUI();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < songList.size() - 1) {
                currentIndex++;
                musicService.playNext();
                updateUI();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    musicService.seekTo(progress);
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
        if (!serviceBound || musicService == null) return;
        
        updateUI();
        musicService.playSong();
    }

    private void updateUI() {
        if (!serviceBound || musicService == null) return;
        
        Song currentSong = musicService.getCurrentSong();
        if (currentSong == null) return;

        tvTitle.setText(currentSong.getTitle());
        tvArtist.setText(currentSong.getArtistId());
        
        Glide.with(this)
                .load(currentSong.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .circleCrop()
                .into(imgCover);

        int duration = musicService.getDuration();
        if (duration > 0) {
            seekBar.setMax(duration);
            tvTotalTime.setText(MusicUtils.formatTime(duration));
        }

        // Gradient động
        View gradientOverlay = findViewById(R.id.gradientOverlay);
        Glide.with(this)
            .asBitmap()
            .load(currentSong.getCoverUrl())
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    GradientUtils.createGradientFromBitmap(resource, gradientOverlay);
                }
                @Override
                public void onLoadCleared(Drawable placeholder) {}
            });

        if (musicService.isPlaying()) {
            startVinylRotation();
            startCoverRotation();
            btnPlayPause.setImageResource(R.drawable.ic_pause_white_36dp);
            handler.postDelayed(updateSeekBar, 1000);
        } else {
            stopVinylRotation();
            stopCoverRotation();
            btnPlayPause.setImageResource(R.drawable.ic_play_white_36dp);
        }
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
        if (!serviceBound || musicService == null) return;
        musicService.resumeSong();
        btnPlayPause.setImageResource(R.drawable.ic_pause_white_36dp);
        startVinylRotation();
        startCoverRotation();
        handler.postDelayed(updateSeekBar, 1000);
    }

    private void pauseMusic() {
        if (!serviceBound || musicService == null) return;
        musicService.pauseSong();
        btnPlayPause.setImageResource(R.drawable.ic_play_white_36dp);
        stopVinylRotation();
        stopCoverRotation();
        handler.removeCallbacks(updateSeekBar);
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (serviceBound && musicService != null && musicService.isPlaying()) {
                int currentPosition = musicService.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                tvCurrentTime.setText(MusicUtils.formatTime(currentPosition));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBar);
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        stopVinylRotation();
        stopCoverRotation();
    }
} 