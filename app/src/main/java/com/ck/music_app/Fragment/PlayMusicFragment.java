package com.ck.music_app.Fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import com.ck.music_app.interfaces.MusicPlayerCallback;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.ViewTreeObserver;
import android.view.ViewGroup.LayoutParams;

import java.util.ArrayList;
import java.util.List;

public class PlayMusicFragment extends Fragment {

    private View rootLayout;
    private View fullPlayerLayout;
    private View miniPlayerLayout;
    private ImageView imgVinyl, imgCover, imgMiniCover, imgMiniVinyl;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private TextView tvMiniTitle, tvMiniArtist;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnBack, btnPrevious, btnNext;
    private ImageButton btnMiniPlayPause, btnMiniPrevious, btnMiniNext;
    private View gradientOverlay;
    private View miniGradientOverlay;

    private List<Song> songList = new ArrayList<>();
    private int currentIndex;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private Handler handler = new Handler();
    private float currentRotating = 0f;
    private boolean isVinylRotating = false;
    private boolean isCoverRotating = false;
    private boolean isMinimized = false;
    private float originalY = 0;
    private float miniPlayerY = 0;
    private MusicPlayerCallback callback;

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
        setupViewTreeObserver();
        initListeners();
        
        // Đảm bảo mini player bị ẩn và full player hiển thị khi mới tạo fragment
        miniPlayerLayout.setVisibility(View.GONE);
        fullPlayerLayout.setVisibility(View.VISIBLE);
        
        loadSong(currentIndex);
        return view;
    }

    private void initViews(View view) {
        rootLayout = view.findViewById(R.id.root_layout);
        fullPlayerLayout = view.findViewById(R.id.full_player_layout);
        miniPlayerLayout = view.findViewById(R.id.mini_player_layout);
        
        imgVinyl = view.findViewById(R.id.imgVinyl);
        imgCover = view.findViewById(R.id.imgCover);
        imgMiniVinyl = view.findViewById(R.id.imgMiniVinyl);
        imgMiniCover = view.findViewById(R.id.imgMiniCover);
        
        tvTitle = view.findViewById(R.id.tvTitle);
        tvArtist = view.findViewById(R.id.tvArtist);
        tvMiniTitle = view.findViewById(R.id.tvMiniTitle);
        tvMiniArtist = view.findViewById(R.id.tvMiniArtist);
        
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        seekBar = view.findViewById(R.id.seekBar);
        
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnMiniPlayPause = view.findViewById(R.id.btnMiniPlayPause);
        btnMiniPrevious = view.findViewById(R.id.btnMiniPrevious);
        btnMiniNext = view.findViewById(R.id.btnMiniNext);
        btnBack = view.findViewById(R.id.btnBack);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        
        gradientOverlay = view.findViewById(R.id.gradientOverlay);
        miniGradientOverlay = view.findViewById(R.id.miniGradientOverlay);
    }

    private void setupViewTreeObserver() {
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                originalY = fullPlayerLayout.getY();
                View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
                miniPlayerY = rootLayout.getHeight() - miniPlayerLayout.getHeight() - bottomNav.getHeight();
                rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void initListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnMiniPlayPause.setOnClickListener(v -> togglePlayPause());

        btnBack.setOnClickListener(v -> minimize());

        miniPlayerLayout.setOnClickListener(v -> maximize());

        btnPrevious.setOnClickListener(v -> playPrevious());
        btnMiniPrevious.setOnClickListener(v -> playPrevious());

        btnNext.setOnClickListener(v -> playNext());
        btnMiniNext.setOnClickListener(v -> playNext());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    if (callback != null) {
                        callback.onProgressChanged(progress, mediaPlayer.getDuration());
                    }
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

    private void togglePlayPause() {
        isPlaying = !isPlaying;
        if (isPlaying) {
            playMusic();
        } else {
            pauseMusic();
        }
        updatePlayPauseButtons();
    }

    private void updatePlayPauseButtons() {
        int icon = isPlaying ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_white_36dp;
        btnPlayPause.setImageResource(icon);
        btnMiniPlayPause.setImageResource(icon);
    }

    public void minimize() {
        if (isMinimized) return;
        isMinimized = true;

        // Lấy chiều cao của màn hình
        int screenHeight = requireActivity().getWindow().getDecorView().getHeight();
        // Lấy chiều cao của mini player
        final int miniHeight = getResources().getDimensionPixelSize(R.dimen.mini_player_height);
        // Lấy chiều cao của bottom navigation
        View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        int bottomNavHeight = bottomNav.getHeight();

        // Animation cho root layout
        ValueAnimator rootHeightAnimator = ValueAnimator.ofInt(screenHeight, miniHeight);
        rootHeightAnimator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = rootLayout.getLayoutParams();
            layoutParams.height = val;
            rootLayout.setLayoutParams(layoutParams);
        });

        // Animation cho full player layout
        ValueAnimator playerHeightAnimator = ValueAnimator.ofInt(screenHeight, miniHeight);
        playerHeightAnimator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = fullPlayerLayout.getLayoutParams();
            layoutParams.height = val;
            fullPlayerLayout.setLayoutParams(layoutParams);
        });

        // Animate the layouts
        AnimatorSet animatorSet = new AnimatorSet();

        // Move to bottom animation
        ObjectAnimator translateY = ObjectAnimator.ofFloat(fullPlayerLayout, "translationY", 0f, miniPlayerY - originalY);
        
        // Fade out full player and fade in mini player
        ObjectAnimator fullPlayerAlpha = ObjectAnimator.ofFloat(fullPlayerLayout, "alpha", 1f, 0f);
        ObjectAnimator miniPlayerAlpha = ObjectAnimator.ofFloat(miniPlayerLayout, "alpha", 0f, 1f);

        animatorSet.playTogether(translateY, fullPlayerAlpha, miniPlayerAlpha, rootHeightAnimator, playerHeightAnimator);
        animatorSet.setDuration(150);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                miniPlayerLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                fullPlayerLayout.setVisibility(View.GONE);
                // Reset lại transform để tránh ảnh hưởng đến lần maximize tiếp theo
                fullPlayerLayout.setTranslationY(0);
                
                // Kiểm tra sau khi animation hoàn thành
                if (miniPlayerLayout.getVisibility() == View.VISIBLE) {
                    startVinylRotation(imgMiniVinyl);
                    startCoverRotation(imgMiniCover);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        animatorSet.start();
    }

    public void maximize() {
        if (!isMinimized) return;
        isMinimized = false;

        // Lấy chiều cao của màn hình
        int screenHeight = requireActivity().getWindow().getDecorView().getHeight();
        // Lấy chiều cao của mini player
        final int miniHeight = getResources().getDimensionPixelSize(R.dimen.mini_player_height);

        // Animation cho root layout
        ValueAnimator rootHeightAnimator = ValueAnimator.ofInt(miniHeight, screenHeight);
        rootHeightAnimator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = rootLayout.getLayoutParams();
            layoutParams.height = val;
            rootLayout.setLayoutParams(layoutParams);
        });

        // Animation cho full player layout
        ValueAnimator playerHeightAnimator = ValueAnimator.ofInt(miniHeight, screenHeight);
        playerHeightAnimator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = fullPlayerLayout.getLayoutParams();
            layoutParams.height = val;
            fullPlayerLayout.setLayoutParams(layoutParams);
        });

        // Animate the layouts
        AnimatorSet animatorSet = new AnimatorSet();

        // Move to original position
        ObjectAnimator translateY = ObjectAnimator.ofFloat(fullPlayerLayout, "translationY", miniPlayerY - originalY, 0f);
        
        // Fade in full player and fade out mini player
        ObjectAnimator fullPlayerAlpha = ObjectAnimator.ofFloat(fullPlayerLayout, "alpha", 0f, 1f);
        ObjectAnimator miniPlayerAlpha = ObjectAnimator.ofFloat(miniPlayerLayout, "alpha", 1f, 0f);

        animatorSet.playTogether(translateY, fullPlayerAlpha, miniPlayerAlpha, rootHeightAnimator, playerHeightAnimator);
        animatorSet.setDuration(150);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                fullPlayerLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                miniPlayerLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        animatorSet.start();
    }

    private void startVinylRotation(ImageView vinyl) {
        if (vinyl != null) {
            vinyl.setRotation(currentRotating);
            vinyl.animate()
                    .rotationBy(360f + currentRotating)
                    .setDuration(8000)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(() -> {
                        if (isVinylRotating) {
                            startVinylRotation(vinyl);
                        }
                    })
                    .start();
        }
    }

    private void stopVinylRotation(ImageView vinyl) {
        if (vinyl != null) {
            vinyl.animate().cancel();
            currentRotating = vinyl.getRotation();
        }
    }

    private void startAllVinylRotations() {
        System.out.println("hahaha");
        isVinylRotating = true;
        startVinylRotation(imgVinyl);
        startVinylRotation(imgMiniVinyl);
    }

    private void stopAllVinylRotations() {
        isVinylRotating = false;
        stopVinylRotation(imgVinyl);
        stopVinylRotation(imgMiniVinyl);
    }

    private void startCoverRotation(ImageView cover) {
        isCoverRotating = true;
        if (cover != null) {
            cover.setRotation(currentRotating);
            cover.animate()
                    .rotationBy(360f + currentRotating)
                    .setDuration(8000)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(() -> {
                        if (isCoverRotating) startCoverRotation(cover);
                    })
                    .start();
        }
    }

    private void stopCoverRotation() {
        isCoverRotating = false;
        imgCover.animate().cancel();
        imgMiniCover.animate().cancel();
        currentRotating = imgCover.getRotation();
    }

    private void loadSong(int index) {
        Song song = songList.get(index);
        
        // Update full player views
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtistId());
        
        // Update mini player views
        tvMiniTitle.setText(song.getTitle());
        tvMiniArtist.setText(song.getArtistId());
        
        // Load images for full player
        Glide.with(this)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .circleCrop()
                .into(imgCover);

        // Load images for mini player
        Glide.with(this)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .circleCrop()
                .into(imgMiniCover);

        // Reset rotation state
        if (isVinylRotating) {
            stopAllVinylRotations();
            stopCoverRotation();
        }
        currentRotating = 0f;
        imgVinyl.setRotation(0f);
        imgMiniVinyl.setRotation(0f);
        imgCover.setRotation(0f);
        imgMiniCover.setRotation(0f);

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

        // Apply gradient for both full player and mini player
        Glide.with(this)
            .asBitmap()
            .load(song.getCoverUrl())
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    GradientUtils.createGradientFromBitmap(resource, gradientOverlay);
                    GradientUtils.createGradientFromBitmap(resource, miniGradientOverlay);
                }

                @Override
                public void onLoadCleared(Drawable placeholder) {}
            });
    }

    private void playMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause_white_36dp);
            btnMiniPlayPause.setImageResource(R.drawable.ic_pause_white_36dp);
            isPlaying = true;
            if (!isMinimized){
                startVinylRotation(imgVinyl);
                startCoverRotation(imgCover);
            }
            else {
                startVinylRotation(imgMiniVinyl);
                startCoverRotation(imgMiniCover);
            }
            handler.postDelayed(updateSeekBar, 1000);
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play_white_36dp);
            btnMiniPlayPause.setImageResource(R.drawable.ic_play_white_36dp);
            isPlaying = false;
            stopAllVinylRotations();
            stopCoverRotation();
            handler.removeCallbacks(updateSeekBar);
        }
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                tvCurrentTime.setText(MusicUtils.formatTime(currentPosition));
                if (callback != null) {
                    callback.onProgressChanged(currentPosition, mediaPlayer.getDuration());
                }
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBar);
        stopAllVinylRotations();
        stopCoverRotation();
    }

    public void setMusicPlayerCallback(MusicPlayerCallback callback) {
        this.callback = callback;
    }

    public void updatePlayingState(boolean isPlaying) {
        this.isPlaying = isPlaying;
        if (isPlaying) {
            playMusic();
        } else {
            pauseMusic();
        }
    }

    public void updateProgress(int progress, int duration) {
        if (seekBar != null) {
            seekBar.setProgress(progress);
            tvCurrentTime.setText(MusicUtils.formatTime(progress));
            tvTotalTime.setText(MusicUtils.formatTime(duration));
        }
    }

    private void playPrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            if (callback != null) {
                callback.onSongChanged(currentIndex);
            }
            loadSong(currentIndex);
        }
    }

    private void playNext() {
        if (currentIndex < songList.size() - 1) {
            currentIndex++;
            if (callback != null) {
                callback.onSongChanged(currentIndex);
            }
            loadSong(currentIndex);
        }
    }
} 