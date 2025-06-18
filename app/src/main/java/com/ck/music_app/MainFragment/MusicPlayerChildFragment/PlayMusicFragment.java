package com.ck.music_app.MainFragment.MusicPlayerChildFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ck.music_app.Dialog.LoadingDialog;
import com.ck.music_app.MainFragment.MusicPlayerFragment;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.utils.GradientUtils;
import com.ck.music_app.utils.MusicUtils;
import com.ck.music_app.Services.FirebaseService;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
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
    private ImageButton btnPlayPause, btnPrevious, btnNext, btnShuffle, btnRepeat, btnDownload;

    private LoadingDialog loadingDialog;

    private List<Song> songList = new ArrayList<>();
    private int currentIndex;
    private boolean isPlaying = false;
    private float currentRotating = 0f;
    private boolean isVinylRotating = false;
    private boolean isCoverRotating = false;
    private LocalBroadcastManager broadcaster;
    private boolean isShuffleOn = false;
    private int repeatMode = 0; // 0: no repeat, 1: repeat all, 2: repeat one

    private List<Song> originalSongList = new ArrayList<>();

    private FirebaseService firebaseService;

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
                case MusicService.BROADCAST_PLAYLIST_CHANGED:
                    updatePlaylistState(
                        (List<Song>) intent.getSerializableExtra("songList"),
                        intent.getIntExtra("currentIndex", 0),
                        intent.getBooleanExtra("isShuffleOn", false)
                    );
                    break;
            }
        }
    };

    public PlayMusicFragment() {
        // Required empty public constructor
    }

    public static PlayMusicFragment newInstance(int currentIndex) {
        PlayMusicFragment fragment = new PlayMusicFragment();
        Bundle args = new Bundle();
        args.putInt("currentIndex", currentIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentIndex = getArguments().getInt("currentIndex", 0);
        }
        broadcaster = LocalBroadcastManager.getInstance(requireContext());
        loadingDialog = new LoadingDialog(requireContext());
        firebaseService = FirebaseService.getInstance();
        registerReceiver();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play_music, container, false);
        initViews(view);
        initListeners();
        
        // Lấy thông tin bài hát hiện tại từ Service
        Song currentSong = MusicService.getCurrentSong();
        if (currentSong != null) {
            updateUI(currentSong);
        }
        
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
        btnShuffle = view.findViewById(R.id.btnShuffle);
        btnRepeat = view.findViewById(R.id.btnRepeat);
        btnDownload = view.findViewById(R.id.btnDownload);
    }

    private void initListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnShuffle.setOnClickListener(v -> toggleShuffle());
        btnRepeat.setOnClickListener(v -> toggleRepeat());
        btnDownload.setOnClickListener(v -> handleDownload());

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
        List<Song> currentPlaylist = MusicService.getCurrentPlaylist();
        if (currentPlaylist != null && !currentPlaylist.isEmpty() && index >= 0 && index < currentPlaylist.size()) {
            Song song = currentPlaylist.get(index);
            updateUI(song);

            // Gửi yêu cầu phát nhạc đến Service
            Intent intent = new Intent(requireContext(), MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY);
            intent.putExtra("position", index);
            requireContext().startService(intent);
        }
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
        currentIndex = position;
        List<Song> currentPlaylist = MusicService.getCurrentPlaylist();
        if (currentPlaylist != null && position < currentPlaylist.size()) {
            Song song = currentPlaylist.get(position);
            updateUI(song);
            
            // Broadcast song info update
            Intent intent = new Intent("UPDATE_SONG_INFO");
            
            String coverUrl = song.getCoverUrl();
            if (coverUrl != null && !coverUrl.isEmpty()) {
                intent.putExtra("COVER_URL", coverUrl);
            } else {
                String defaultCover = "android.resource://" + requireContext().getPackageName() + "/mipmap/ic_launcher_new";
                intent.putExtra("COVER_URL", defaultCover);
            }
            
            String lyrics = song.getLyrics();
            if (lyrics != null && !lyrics.isEmpty()) {
                intent.putExtra("LYRIC", lyrics);
            } else {
                intent.putExtra("LYRIC", "Chưa có lời bài hát");
            }
            
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        }
    }

    private void updateProgress(int progress, int duration) {
        seekBar.setMax(duration);
        seekBar.setProgress(progress);
        tvCurrentTime.setText(MusicUtils.formatTime(progress));
        tvTotalTime.setText(MusicUtils.formatTime(duration));
    }

    private void updateUI(Song song) {
        tvTitle.setText(song.getTitle());
        
        // Get artist name from Firebase
        String artistId = song.getArtistId();
        if (artistId != null && !artistId.isEmpty()) {
            firebaseService.getArtistNameById(artistId, new FirebaseService.FirestoreCallback<String>() {
                @Override
                public void onSuccess(String artistName) {
                    tvArtist.setText(artistName);
                }

                @Override
                public void onError(Exception e) {
                    tvArtist.setText("Unknown Artist");
                }
            });
        } else {
            tvArtist.setText("Unknown Artist");
        }

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
        filter.addAction(MusicService.BROADCAST_PLAYLIST_CHANGED);
        broadcaster.registerReceiver(musicReceiver, filter);
    }

    private void updatePlaylistState(List<Song> newSongList, int newIndex, boolean isShuffleOn) {
        // Cập nhật UI cho bài hát hiện tại nếu cần
        btnShuffle.setSelected(isShuffleOn);
        if (currentIndex != newIndex) {
            currentIndex = newIndex;
            Song currentSong = newSongList.get(newIndex);
            updateUI(currentSong);
        }
    }

    private void toggleShuffle() {
        isShuffleOn = !isShuffleOn;
        btnShuffle.setSelected(isShuffleOn);
        System.out.println(isShuffleOn);
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_TOGGLE_SHUFFLE);
        intent.putExtra("isShuffleOn", isShuffleOn); // Đảo ngược trạng thái hiện tại
        requireContext().startService(intent);
    }

    private void toggleRepeat() {
        repeatMode = (repeatMode + 1) % 3; // Chuyển qua lại giữa 3 chế độ
        btnRepeat.setSelected(repeatMode > 0);

        // Cập nhật icon và gửi trạng thái đến Service
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_TOGGLE_REPEAT);
        intent.putExtra("repeatMode", repeatMode);
        requireContext().startService(intent);

        // Cập nhật UI và hiển thị thông báo
        String message;
        switch (repeatMode) {
            case 0: // No repeat
                btnRepeat.setImageResource(R.drawable.ic_no_repeat_white_24dp);
                message = "Tắt lặp lại";
                break;
            case 1: // Repeat all
                btnRepeat.setImageResource(R.drawable.ic_repeat_white_24dp);
                message = "Lặp lại tất cả";
                break;
            case 2: // Repeat one
                btnRepeat.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                message = "Lặp lại một bài";
                break;
            default:
                message = "";
                break;
        }

        if (!message.isEmpty()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDownload() {
        // Lấy thông tin bài hát hiện tại
        Song currentSong = MusicService.getCurrentSong();
        if (currentSong == null) {
            Toast.makeText(getContext(), "Không có bài hát nào đang phát", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gọi hàm download từ MusicPlayerFragment
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof MusicPlayerFragment) {
            btnDownload.setSelected(true);
            ((MusicPlayerFragment) parentFragment).handleDownload(currentSong);
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}