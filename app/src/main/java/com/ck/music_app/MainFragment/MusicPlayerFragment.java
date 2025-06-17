package com.ck.music_app.MainFragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ck.music_app.Dialog.LoadingDialog;
import com.ck.music_app.MainFragment.MusicPlayerChildFragment.LyricFragment;
import com.ck.music_app.MainFragment.MusicPlayerChildFragment.PlayMusicFragment;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.Viewpager.MusicPlayerPagerAdapter;
import com.ck.music_app.utils.GradientUtils;
import com.ck.music_app.utils.DownloadUtils;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Build;

public class MusicPlayerFragment extends Fragment {
    private View rootLayout;
    private View fullPlayerLayout;
    private View miniPlayerLayout;

    private View topLayout;
    private ImageView imgMiniCover;
    private ImageView imgMiniVinyl;
    private TextView tvMiniTitle, tvMiniArtist, tvAlbumName;
    private ImageButton btnMiniPlayPause, btnMiniPrevious, btnMiniNext, btnBack;
    private View miniGradientOverlay, gradientOverlay;
    private LoadingDialog loadingDialog;

    private Fragment[] fragments;
    private ViewPager2 viewPager;
    private List<Song> songList = new ArrayList<>();
    private int currentIndex;
    private String albumName;
    private boolean isPlaying = false;
    private float currentRotating = 0f;
    private boolean isVinylRotating = false;
    private boolean isCoverRotating = false;
    private boolean isMinimized = false;
    private float originalY = 0;
    private float miniPlayerY = 0;
    private LocalBroadcastManager broadcaster;

    private View downloadProgressView;
    private ProgressBar progressBar;
    private TextView tvDownloadPercent, tvDownloadTitle;
    private ImageButton btnCancelDownload;
    private Thread downloadThread;
    private boolean isDownloading = false;

    private PlayMusicFragment playMusicFragment;
    private LyricFragment lyricFragment;
    private boolean isPlayerVisible = false;

    private final BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MusicService.BROADCAST_PLAYING_STATE:
                    boolean playing = intent.getBooleanExtra("isPlaying", false);
                    updatePlayingState(playing);
                    break;
                case MusicService.BROADCAST_SONG_CHANGED:
                    int position = intent.getIntExtra("position", 0);
                    updateCurrentSong(position);
                    break;
                case MusicService.BROADCAST_LOADING_STATE:
                    boolean isLoading = intent.getBooleanExtra("isLoading", false);
                    updateLoadingState(isLoading);
                    break;
            }
        }
    };

    public static MusicPlayerFragment newInstance(List<Song> songs, int currentIndex, String albumName) {
        MusicPlayerFragment fragment = new MusicPlayerFragment();
        Bundle args = new Bundle();
        args.putSerializable("songList", new ArrayList<>(songs));
        args.putInt("currentIndex", currentIndex);
        args.putString("albumName", albumName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songList = (List<Song>) getArguments().getSerializable("songList");
            currentIndex = getArguments().getInt("currentIndex", 0);
            albumName = getArguments().getString("albumName", "Unknown Album");
        }
        broadcaster = LocalBroadcastManager.getInstance(requireContext());
        loadingDialog = new LoadingDialog(requireContext());
        registerReceiver();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_player, container, false);
        initViews(view);
        setupViewTreeObserver();
        initListeners();

        // Set album name
        tvAlbumName.setText(albumName);

        fragments = new Fragment[]{
                PlayMusicFragment.newInstance(currentIndex),
                new LyricFragment()
        };

        viewPager = view.findViewById(R.id.view_pager);
        MusicPlayerPagerAdapter adapter = new MusicPlayerPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0, false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
            }
        });

        updateMiniPlayer(songList.get(currentIndex));

        // Khởi tạo view download
        initializeDownloadView(view);
        
        return view;
    }

    private void initViews(View view) {
        rootLayout = view.findViewById(R.id.root_layout);
        topLayout = view.findViewById(R.id.top_layout);
        fullPlayerLayout = view.findViewById(R.id.fragment_container);
        miniPlayerLayout = view.findViewById(R.id.mini_player_layout);
        imgMiniVinyl = view.findViewById(R.id.imgMiniVinyl);
        imgMiniCover = view.findViewById(R.id.imgMiniCover);
        tvMiniTitle = view.findViewById(R.id.tvMiniTitle);
        tvMiniArtist = view.findViewById(R.id.tvMiniArtist);
        tvAlbumName = view.findViewById(R.id.tvAlbumName);
        btnMiniPlayPause = view.findViewById(R.id.btnMiniPlayPause);
        btnMiniPrevious = view.findViewById(R.id.btnMiniPrevious);
        btnMiniNext = view.findViewById(R.id.btnMiniNext);
        btnBack = view.findViewById(R.id.btnBack);
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
        btnMiniPlayPause.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MusicService.class);
            intent.setAction(isPlaying ? MusicService.ACTION_PAUSE : MusicService.ACTION_PLAY);
            requireContext().startService(intent);
        });
        miniPlayerLayout.setOnClickListener(v -> maximize());
        btnMiniPlayPause.setOnClickListener(v -> togglePlayPause());
        btnMiniPrevious.setOnClickListener(v -> playPrevious());
        btnMiniNext.setOnClickListener(v -> playNext());
        btnBack.setOnClickListener(v -> minimize());
    }

    private void updateLoadingState(boolean isLoading) {
        if (isLoading) {
            loadingDialog.show();
        } else {
            loadingDialog.dismiss();
        }
    }

    private void playPrevious() {
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_PREVIOUS);
        requireContext().startService(intent);
                    }

    private void playNext() {
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_NEXT);
        requireContext().startService(intent);
    }

    private void togglePlayPause() {
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(isPlaying ? MusicService.ACTION_PAUSE : MusicService.ACTION_PLAY);
        requireContext().startService(intent);
    }

    public void minimize() {
        if (isMinimized) return;
        isMinimized = true;

        int screenHeight = requireActivity().getWindow().getDecorView().getHeight();
        final int miniHeight = getResources().getDimensionPixelSize(R.dimen.mini_player_height);
        View bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
        int bottomNavHeight = bottomNav.getHeight();

        ValueAnimator rootHeightAnimator = ValueAnimator.ofInt(screenHeight, miniHeight);
        rootHeightAnimator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = rootLayout.getLayoutParams();
            layoutParams.height = val;
            rootLayout.setLayoutParams(layoutParams);
        });

        ValueAnimator playerHeightAnimator = ValueAnimator.ofInt(screenHeight, miniHeight);
        playerHeightAnimator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = fullPlayerLayout.getLayoutParams();
            layoutParams.height = val;
            fullPlayerLayout.setLayoutParams(layoutParams);
        });

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator translateY = ObjectAnimator.ofFloat(fullPlayerLayout, "translationY", 0f, miniPlayerY - originalY);
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
                topLayout.setVisibility(View.GONE);
                fullPlayerLayout.setTranslationY(0);
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

        int screenHeight = requireActivity().getWindow().getDecorView().getHeight();
        final int miniHeight = getResources().getDimensionPixelSize(R.dimen.mini_player_height);

        ValueAnimator rootHeightAnimator = ValueAnimator.ofInt(miniHeight, screenHeight);
        rootHeightAnimator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = rootLayout.getLayoutParams();
            layoutParams.height = val;
            rootLayout.setLayoutParams(layoutParams);
        });

        ValueAnimator playerHeightAnimator = ValueAnimator.ofInt(miniHeight, screenHeight);
        playerHeightAnimator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = fullPlayerLayout.getLayoutParams();
            layoutParams.height = val;
            fullPlayerLayout.setLayoutParams(layoutParams);
        });

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator translateY = ObjectAnimator.ofFloat(fullPlayerLayout, "translationY", miniPlayerY - originalY, 0f);
        ObjectAnimator fullPlayerAlpha = ObjectAnimator.ofFloat(fullPlayerLayout, "alpha", 0f, 1f);
        ObjectAnimator miniPlayerAlpha = ObjectAnimator.ofFloat(miniPlayerLayout, "alpha", 1f, 0f);

        animatorSet.playTogether(translateY, fullPlayerAlpha, miniPlayerAlpha, rootHeightAnimator, playerHeightAnimator);
        animatorSet.setDuration(150);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                fullPlayerLayout.setVisibility(View.VISIBLE);
                topLayout.setVisibility(View.VISIBLE);
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

    private void updateMiniPlayer(Song song) {
        tvMiniTitle.setText(song.getTitle());
        tvMiniArtist.setText(song.getArtistId());

        Glide.with(this)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .circleCrop()
                .into(imgMiniCover);

        Glide.with(this)
                .asBitmap()
                .load(song.getCoverUrl())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        GradientUtils.createGradientFromBitmap(resource, miniGradientOverlay);
                        GradientUtils.createGradientFromBitmap(resource, gradientOverlay);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {}
                });
    }

    private void updateCurrentSong(int position) {
        currentIndex = position;
        Song song = songList.get(position);
        updateMiniPlayer(song);
    }

    private void updatePlayingState(boolean playing) {
        isPlaying = playing;
        int icon = playing ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_white_36dp;
        btnMiniPlayPause.setImageResource(icon);
        
        if (playing) {
            startVinylRotation(imgMiniVinyl);
            startCoverRotation(imgMiniCover);
        } else {
            stopVinylRotation(imgMiniVinyl);
            stopCoverRotation();
        }
    }

    private void startVinylRotation(ImageView vinyl) {
        isVinylRotating = true;
        if (vinyl != null) {
            vinyl.setRotation(currentRotating);
            vinyl.animate()
                    .rotationBy(360f + currentRotating)
                    .setDuration(8000)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(() -> {
                        if (isVinylRotating) startVinylRotation(vinyl);
                    })
                    .start();
        }
    }

    private void stopVinylRotation(ImageView vinyl) {
        isVinylRotating = false;
        if (vinyl != null) {
            vinyl.animate().cancel();
            currentRotating = vinyl.getRotation();
        }
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
        imgMiniCover.animate().cancel();
        currentRotating = imgMiniCover.getRotation();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.BROADCAST_PLAYING_STATE);
        filter.addAction(MusicService.BROADCAST_SONG_CHANGED);
        filter.addAction(MusicService.BROADCAST_LOADING_STATE);
        broadcaster.registerReceiver(musicReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        broadcaster.unregisterReceiver(musicReceiver);
        stopVinylRotation(imgMiniVinyl);
        stopCoverRotation();
    }

    public List<Song> getSongList() {
        return songList;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void updateAlbumName(String newAlbumName) {
        this.albumName = newAlbumName;
        if (tvAlbumName != null) {
            tvAlbumName.setText(newAlbumName);
        }
    }

    public void updatePlayerInfo(List<Song> newSongList, int newIndex) {
        // Cập nhật danh sách và vị trí bài hát
        this.songList = new ArrayList<>(newSongList);
        this.currentIndex = newIndex;

        // Cập nhật giao diện mini player
        Song song = songList.get(currentIndex);
        updateMiniPlayer(song);

        // Gửi danh sách mới đến Service
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra("songList", new ArrayList<>(songList));
        intent.putExtra("position", currentIndex);
        requireContext().startService(intent);
    }

    private void initializeDownloadView(View view) {
        // Khởi tạo view tiến trình
        downloadProgressView = LayoutInflater.from(requireContext())
                .inflate(R.layout.download_progress_view, null, false);
        progressBar = downloadProgressView.findViewById(R.id.progressBar);
        tvDownloadPercent = downloadProgressView.findViewById(R.id.tvDownloadPercent);
        tvDownloadTitle = downloadProgressView.findViewById(R.id.tvDownloadTitle);
        btnCancelDownload = downloadProgressView.findViewById(R.id.btnCancelDownload);

        // Thêm view tiến trình vào container
        FrameLayout downloadContainer = view.findViewById(R.id.downloadContainer);
        downloadContainer.addView(downloadProgressView);
        downloadProgressView.setVisibility(View.GONE);

        // Thêm animation cho view download
        downloadProgressView.setTranslationY(200);
        downloadProgressView.setAlpha(0f);

        btnCancelDownload.setOnClickListener(v -> cancelDownload());
    }

    public void handleDownload(Song song) {
        if (isDownloading) {
            Toast.makeText(getContext(), "Đang tải xuống...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (song == null || song.getAudioUrl() == null) {
            Toast.makeText(getContext(), "Không có bài hát nào đang phát", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra nếu URL bắt đầu bằng "content://" thì là bài hát local
        if (song.getAudioUrl().startsWith("content://")) {
            Toast.makeText(getContext(), "Bài hát đã được tải về thiết bị", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra quyền truy cập bộ nhớ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 1001);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
                return;
            }
        }

        // Bắt đầu tải xuống
        isDownloading = true;
        showDownloadProgress();

        downloadThread = new Thread(() -> {
            DownloadUtils.downloadSong(
                requireContext(),
                song.getAudioUrl(),
                song.getTitle(),
                song.getArtistId(),
                song.getCoverUrl(),
                song.getLyrics(),
                new DownloadUtils.DownloadCallback() {
                    @Override
                    public void onProgressUpdate(int progress) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new RuntimeException("Download cancelled");
                        }
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setProgress(progress);
                            tvDownloadPercent.setText(progress + "%");
                            tvDownloadTitle.setText("Đang tải: " + song.getTitle());
                        });
                    }

                    @Override
                    public void onDownloadComplete(String filePath) {
                        requireActivity().runOnUiThread(() -> {
                            hideDownloadProgress();
                            Toast.makeText(getContext(), "Tải xuống thành công!", Toast.LENGTH_SHORT).show();
                            isDownloading = false;
                        });
                    }

                    @Override
                    public void onError(String message) {
                        requireActivity().runOnUiThread(() -> {
                            hideDownloadProgress();
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                            isDownloading = false;
                        });
                    }
                }
            );
        });
        downloadThread.start();
    }

    private void showDownloadProgress() {
        downloadProgressView.setVisibility(View.VISIBLE);
        downloadProgressView.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(300)
                .start();
    }

    private void hideDownloadProgress() {
        downloadProgressView.animate()
                .translationY(200)
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> downloadProgressView.setVisibility(View.GONE))
                .start();
    }

    private void cancelDownload() {
        if (downloadThread != null) {
            downloadThread.interrupt();
        }
        hideDownloadProgress();
        isDownloading = false;
        Toast.makeText(getContext(), "Đã hủy tải xuống", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isDownloading) {
            cancelDownload();
        }
    }

    public void showPlayer(List<Song> songList, int currentIndex, String albumName) {
        this.albumName = albumName;
        this.songList = new ArrayList<>(songList);
        this.currentIndex = currentIndex;
        
        // Gửi danh sách phát mới đến Service
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra("songList", new ArrayList<>(songList));
        intent.putExtra("position", currentIndex);
        requireContext().startService(intent);

        // Cập nhật giao diện mini player
        updateMiniPlayer(songList.get(currentIndex));
        
        // Cập nhật tên album
        if (tvAlbumName != null) {
            tvAlbumName.setText(albumName);
        }
    }
} 