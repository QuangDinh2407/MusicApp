package com.ck.music_app.MainFragment.MusicPlayerChildFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ck.music_app.Dialog.LoadingDialog;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.utils.GradientUtils;
import com.ck.music_app.utils.MusicUtils;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlayMusicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayMusicFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View rootLayout;
    private ImageView imgVinyl, imgCover;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrevious, btnNext;

    private LoadingDialog loadingDialog;

    private List<Song> songList = new ArrayList<>();
    private int currentIndex;
    private boolean isPlaying = false;
    private float currentRotating = 0f;
    private boolean isVinylRotating = false;
    private boolean isCoverRotating = false;
    private LocalBroadcastManager broadcaster;

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
                case MusicService.BROADCAST_PROGRESS:
                    int progress = intent.getIntExtra("progress", 0);
                    int duration = intent.getIntExtra("duration", 0);
                    updateProgress(progress, duration);
                    break;
                case MusicService.BROADCAST_LOADING_STATE:
                    boolean isLoading = intent.getBooleanExtra("isLoading", false);
                    updateLoadingState(isLoading);
                    break;
            }
        }
    };

    public PlayMusicFragment() {
        // Required empty public constructor
    }

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
        broadcaster = LocalBroadcastManager.getInstance(requireContext());
        loadingDialog = new LoadingDialog(requireContext());
        registerReceiver();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play_music, container, false);
        initViews(view);
        initListeners();
        loadSong(currentIndex);
        return view;
    }

    private void initViews(View view) {
        rootLayout = view.findViewById(R.id.root_layout);
        imgVinyl = view.findViewById(R.id.imgVinyl);
        imgCover = view.findViewById(R.id.imgCover);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvArtist = view.findViewById(R.id.tvArtist);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        seekBar = view.findViewById(R.id.seekBar);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
    }

    private void initListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(MusicUtils.formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Có thể thêm code xử lý khi bắt đầu kéo seekbar nếu cần
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Gửi vị trí mới đến Service khi người dùng thả seekbar
                Intent intent = new Intent(requireContext(), MusicService.class);
                intent.setAction(MusicService.ACTION_SEEK);
                intent.putExtra("position", seekBar.getProgress());
                requireContext().startService(intent);
            }
        });
    }

    private void togglePlayPause() {
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(isPlaying ? MusicService.ACTION_PAUSE : MusicService.ACTION_PLAY);
        requireContext().startService(intent);
    }

    private void startVinylRotation() {
        isVinylRotating = true;
        if (imgVinyl != null) {
            imgVinyl.setRotation(currentRotating);
            imgVinyl.animate()
                    .rotationBy(360f + currentRotating)
                    .setDuration(8000)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(() -> {
                        if (isVinylRotating) {
                            startVinylRotation();
                        }
                    })
                    .start();
        }
    }

    private void stopVinylRotation() {
        isVinylRotating = false;
        if (imgVinyl != null) {
            imgVinyl.animate().cancel();
            currentRotating = imgVinyl.getRotation();
        }
    }

    private void startCoverRotation() {
        isCoverRotating = true;
        if (imgCover != null) {
            imgCover.setRotation(currentRotating);
            imgCover.animate()
                    .rotationBy(360f + currentRotating)
                    .setDuration(8000)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(() -> {
                        if (isCoverRotating) {
                            startCoverRotation();
                        }
                    })
                    .start();
        }
    }

    private void stopCoverRotation() {
        isCoverRotating = false;
        if (imgCover != null) {
            imgCover.animate().cancel();
            currentRotating = imgCover.getRotation();
        }
    }

    private void loadSong(int index) {
        // Thêm bounds checking để tránh IndexOutOfBoundsException
        if (songList == null || songList.isEmpty() || index < 0 || index >= songList.size()) {
            return;
        }

        Song song = songList.get(index);

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtistId());

        Glide.with(this)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .circleCrop()
                .into(imgCover);

        // Reset rotation state
        if (isVinylRotating) {
            stopVinylRotation();
            stopCoverRotation();
        }
        currentRotating = 0f;
        imgVinyl.setRotation(0f);
        imgCover.setRotation(0f);

        // Start playing the song through service
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra("songList", new ArrayList<>(songList));
        intent.putExtra("position", index);
        requireContext().startService(intent);

    }

    private void updateLoadingState(boolean isLoading) {
        if (isLoading) {
            loadingDialog.show();
        } else {
            loadingDialog.dismiss();
        }
    }

    private void updatePlayingState(boolean playing) {
        isPlaying = playing;
        int icon = playing ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_white_36dp;
        btnPlayPause.setImageResource(icon);

        if (playing) {
            startVinylRotation();
            startCoverRotation();
        } else {
            stopVinylRotation();
            stopCoverRotation();
        }
    }

    private void updateCurrentSong(int position) {
        // Thêm bounds checking để tránh IndexOutOfBoundsException
        if (songList == null || songList.isEmpty() || position < 0 || position >= songList.size()) {
            return;
        }

        currentIndex = position;
        Song song = songList.get(position);
        updateUI(song);

        // Broadcast song info update
        Intent intent = new Intent("UPDATE_SONG_INFO");
        intent.putExtra("COVER_URL", song.getCoverUrl());
        intent.putExtra("LYRIC", song.getLyrics());
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
    }

    private void updateProgress(int progress, int duration) {
        seekBar.setMax(duration);
        seekBar.setProgress(progress);
        tvCurrentTime.setText(MusicUtils.formatTime(progress));
        tvTotalTime.setText(MusicUtils.formatTime(duration));
    }

    private void updateUI(Song song) {
        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtistId());

        Glide.with(this)
                .load(song.getCoverUrl())
                .placeholder(R.mipmap.ic_launcher)
                .circleCrop()
                .into(imgCover);

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

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.BROADCAST_PLAYING_STATE);
        filter.addAction(MusicService.BROADCAST_SONG_CHANGED);
        filter.addAction(MusicService.BROADCAST_PROGRESS);
        filter.addAction(MusicService.BROADCAST_LOADING_STATE);
        broadcaster.registerReceiver(musicReceiver, filter);
    }

    public void updateSongList(List<Song> newSongList, int newIndex) {
        this.songList = new ArrayList<>(newSongList);
        this.currentIndex = newIndex;

        // Thêm bounds checking trước khi load song
        if (songList != null && !songList.isEmpty() && currentIndex >= 0 && currentIndex < songList.size()) {
            loadSong(currentIndex);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        broadcaster.unregisterReceiver(musicReceiver);
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_STOP);
        requireContext().startService(intent);
        stopVinylRotation();
        stopCoverRotation();
    }
}