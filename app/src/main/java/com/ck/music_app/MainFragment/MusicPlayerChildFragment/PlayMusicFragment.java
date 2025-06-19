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
import com.ck.music_app.utils.PlaylistDialogUtils;

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
    private ImageButton btnPlayPause, btnPrevious, btnNext, btnShuffle, btnRepeat, btnDownload, btnAddToPlaylist;

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
                            intent.getBooleanExtra("isShuffleOn", false));
                    break;
            }
        }
    };

    public PlayMusicFragment() {
        // Required empty public constructor
    }

    // sửa lại theo hàm này
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play_music, container, false);
        initViews(view);
        initListeners();

        // **SỬA: Lấy thông tin bài hát từ parent fragment trước**
        try {
            Fragment parentFragment = getParentFragment();
            if (parentFragment instanceof MusicPlayerFragment) {
                MusicPlayerFragment musicPlayerFragment = (MusicPlayerFragment) parentFragment;
                List<Song> parentSongList = musicPlayerFragment.getSongList();
                int parentCurrentIndex = musicPlayerFragment.getCurrentIndex();

                if (parentSongList != null && !parentSongList.isEmpty() &&
                        parentCurrentIndex >= 0 && parentCurrentIndex < parentSongList.size()) {
                    // Cập nhật từ parent fragment
                    this.songList = new ArrayList<>(parentSongList);
                    this.currentIndex = parentCurrentIndex;
                    Song currentSong = parentSongList.get(parentCurrentIndex);
                    updateUI(currentSong);
                    Log.d("PlayMusicFragment", "Updated UI from parent fragment: " + currentSong.getTitle());
                    return view;
                }
            }
        } catch (Exception e) {
            Log.e("PlayMusicFragment", "Error getting data from parent fragment: " + e.getMessage());
        }

        // **FALLBACK: Lấy thông tin bài hát hiện tại từ Service nếu không có từ
        // parent**
        try {
            Song currentSong = MusicService.getCurrentSongStatic();
            if (currentSong != null) {
                updateUI(currentSong);
                Log.d("PlayMusicFragment", "Updated UI from MusicService: " + currentSong.getTitle());
            } else {
                Log.w("PlayMusicFragment", "No current song available from service or parent");
            }
        } catch (Exception e) {
            Log.e("PlayMusicFragment", "Error getting current song from service: " + e.getMessage());
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
        btnAddToPlaylist = view.findViewById(R.id.btnAddToPlaylist);
    }

    private void initListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnShuffle.setOnClickListener(v -> toggleShuffle());
        btnRepeat.setOnClickListener(v -> toggleRepeat());
        btnDownload.setOnClickListener(v -> handleDownload());
        btnAddToPlaylist.setOnClickListener(v -> showAddToPlaylistDialog());

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
        if (!isAdded() || getContext() == null || imgVinyl == null)
            return;

        isVinylRotating = true;
        try {
            imgVinyl.setRotation(currentRotating);
            imgVinyl.animate()
                    .rotationBy(360f + currentRotating)
                    .setDuration(8000)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(() -> {
                        if (isVinylRotating && isAdded() && getContext() != null) {
                            startVinylRotation();
                        }
                    })
                    .start();
        } catch (Exception e) {
            Log.e("PlayMusicFragment", "Error starting vinyl rotation: " + e.getMessage());
        }
    }

    private void stopVinylRotation() {
        isVinylRotating = false;
        try {
            if (imgVinyl != null && isAdded() && getContext() != null) {
                imgVinyl.animate().cancel();
                currentRotating = imgVinyl.getRotation();
            }
        } catch (Exception e) {
            Log.e("PlayMusicFragment", "Error stopping vinyl rotation: " + e.getMessage());
        }
    }

    private void startCoverRotation() {
        if (!isAdded() || getContext() == null || imgCover == null)
            return;

        isCoverRotating = true;
        try {
            imgCover.setRotation(currentRotating);
            imgCover.animate()
                    .rotationBy(360f + currentRotating)
                    .setDuration(8000)
                    .setInterpolator(new LinearInterpolator())
                    .withEndAction(() -> {
                        if (isCoverRotating && isAdded() && getContext() != null) {
                            startCoverRotation();
                        }
                    })
                    .start();
        } catch (Exception e) {
            Log.e("PlayMusicFragment", "Error starting cover rotation: " + e.getMessage());
        }
    }

    private void stopCoverRotation() {
        isCoverRotating = false;
        try {
            if (imgCover != null && isAdded() && getContext() != null) {
                imgCover.animate().cancel();
                currentRotating = imgCover.getRotation();
            }
        } catch (Exception e) {
            Log.e("PlayMusicFragment", "Error stopping cover rotation: " + e.getMessage());
        }
    }

    private void loadSong(int index) {
        try {
            // Thêm bounds checking để tránh IndexOutOfBoundsException
            if (songList == null || songList.isEmpty() || index < 0 || index >= songList.size()) {
                Log.w("PlayMusicFragment", "Invalid song list or index in loadSong");
                return;
            }

            Song song = songList.get(index);
            if (song == null) {
                Log.w("PlayMusicFragment", "Song is null at index: " + index);
                return;
            }

            // Cập nhật UI với bài hát mới
            updateUI(song);

            // Reset rotation state
            if (isVinylRotating) {
                stopVinylRotation();
                stopCoverRotation();
            }
            currentRotating = 0f;
            if (imgVinyl != null) {
                imgVinyl.setRotation(0f);
            }
            if (imgCover != null) {
                imgCover.setRotation(0f);
            }

            // Start playing the song through service
            try {
                if (getContext() != null) {
                    Intent intent = new Intent(getContext(), MusicService.class);
                    intent.setAction(MusicService.ACTION_PLAY);
                    intent.putExtra("songList", new ArrayList<>(songList));
                    intent.putExtra("position", index);
                    getContext().startService(intent);
                    Log.d("PlayMusicFragment", "Started service for song: " + song.getTitle());
                }
            } catch (Exception e) {
                Log.e("PlayMusicFragment", "Error starting music service: " + e.getMessage());
            }

        } catch (Exception e) {
            Log.e("PlayMusicFragment", "Error in loadSong: " + e.getMessage(), e);
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
        // Sử dụng songList local thay vì lấy từ service
        if (songList != null && position >= 0 && position < songList.size()) {
            Song song = songList.get(position);
            updateUI(song);

            // Broadcast song info update
            Intent intent = new Intent("UPDATE_SONG_INFO");

            String coverUrl = song.getCoverUrl();
            if (coverUrl != null && !coverUrl.isEmpty()) {
                intent.putExtra("COVER_URL", coverUrl);
            } else {
                String defaultCover = "android.resource://" + requireContext().getPackageName()
                        + "/mipmap/ic_launcher_new";
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

    private void handleAddToPlaylist() {
        // Lấy thông tin bài hát hiện tại
        Song currentSong = MusicService.getCurrentSong();
        if (currentSong == null) {
            Toast.makeText(getContext(), "Không có bài hát nào đang phát", Toast.LENGTH_SHORT).show();
            return;
        }

        showAddToPlaylistDialog();
    }

    private void handleAddToPlaylist() {
        // Lấy thông tin bài hát hiện tại
        Song currentSong = MusicService.getCurrentSong();
        if (currentSong == null) {
            Toast.makeText(getContext(), "Không có bài hát nào đang phát", Toast.LENGTH_SHORT).show();
            return;
        }

        showAddToPlaylistDialog();
    }

    private void handleDownload() {
        // Lấy thông tin bài hát hiện tại
        Song currentSong = MusicService.getCurrentSongStatic();
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

    private void showAddToPlaylistDialog() {
        // Get current song
        Song currentSong = MusicService.getCurrentSong();
        if (currentSong == null) {
            Toast.makeText(getContext(), "Không có bài hát nào đang phát", Toast.LENGTH_SHORT).show();
            return;
        }

        PlaylistDialogUtils.showAddToPlaylistDialog(requireContext(), currentSong);
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

        // Cancel any ongoing animations
        try {
            if (imgVinyl != null) {
                imgVinyl.animate().cancel();
            }
            if (imgCover != null) {
                imgCover.animate().cancel();
            }
        } catch (Exception e) {
            // Ignore
        }

        // Clear view references
        imgVinyl = null;
        imgCover = null;
        btnPlayPause = null;
        seekBar = null;
    }

}
