package com.ck.music_app;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ck.music_app.MainFragment.HomeFragment;
import com.ck.music_app.MainFragment.LibraryFragment;
import com.ck.music_app.MainFragment.ProfileFragment;
import com.ck.music_app.MainFragment.SearchFragment;
import com.ck.music_app.Model.Song;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.Services.FirebaseService;
import com.ck.music_app.Viewpager.MainPagerAdapter;
import com.ck.music_app.utils.FirestoreUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Fragment[] fragments;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseService firebaseService;
    private View miniPlayerContainer;
    private ImageView miniPlayerCover;
    private TextView miniPlayerTitle, miniPlayerArtist;
    private ImageButton miniPlayerPlayPause, miniPlayerNext;
    private ProgressBar miniPlayerProgress;
    private MusicService musicService;
    private boolean serviceBound = false;
    private Handler progressHandler = new Handler();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            updateMiniPlayer();
            // Load current song from Firebase after service is connected
            loadCurrentSongFromFirebase();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private BroadcastReceiver musicUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("MUSIC_UPDATE".equals(intent.getAction())) {
                updateMiniPlayer();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Đăng ký BroadcastReceiver với flag phù hợp cho Android 14+
        IntentFilter filter = new IntentFilter("MUSIC_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(musicUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(musicUpdateReceiver, filter);
        }

        // Lấy email từ Intent và hiển thị Toast
        String email = getIntent().getStringExtra("email");
        if (email != null && !email.isEmpty()) {
            Toast.makeText(this, "Xin chào: " + email, Toast.LENGTH_LONG).show();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        firebaseService = FirebaseService.getInstance();
        testFirestoreUtils();

        initViews();
        initMiniPlayer();
        initListeners();
        
        // Bind to MusicService
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews() {
        fragments = new Fragment[]{
                new HomeFragment(),
                new SearchFragment(),
                new LibraryFragment(),
                new ProfileFragment()
        };

        viewPager = findViewById(R.id.view_pager);
        MainPagerAdapter adapter = new MainPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0, false);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void initMiniPlayer() {
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        miniPlayerCover = findViewById(R.id.mini_player_cover);
        miniPlayerTitle = findViewById(R.id.mini_player_title);
        miniPlayerArtist = findViewById(R.id.mini_player_artist);
        miniPlayerPlayPause = findViewById(R.id.mini_player_play_pause);
        miniPlayerNext = findViewById(R.id.mini_player_next);
        miniPlayerProgress = findViewById(R.id.mini_player_progress);

        // Click listeners for mini player
        miniPlayerContainer.setOnClickListener(v -> {
            if (serviceBound && musicService.getCurrentSong() != null) {
                Intent intent = new Intent(MainActivity.this, PlayMusicActivity.class);
                intent.putExtra("songList", (java.io.Serializable) musicService.getSongList());
                intent.putExtra("currentIndex", musicService.getCurrentIndex());
                startActivity(intent);
            }
        });

        miniPlayerPlayPause.setOnClickListener(v -> {
            if (serviceBound) {
                if (musicService.isPlaying()) {
                    musicService.pauseSong();
                    miniPlayerPlayPause.setImageResource(R.drawable.ic_play_white_36dp);
                    stopProgressUpdate();
                } else {
                    musicService.resumeSong();
                    miniPlayerPlayPause.setImageResource(R.drawable.ic_pause_white_36dp);
                    startProgressUpdate();
                }
            }
        });

        miniPlayerNext.setOnClickListener(v -> {
            if (serviceBound) {
                musicService.playNext();
                updateMiniPlayer();
            }
        });
    }

    private void initListeners() {
        // Khi vuốt ViewPager2 thì đổi tab menu
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });

        // Khi bấm menu thì đổi trang ViewPager2
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    viewPager.setCurrentItem(0);
                    break;
                case R.id.nav_search:
                    viewPager.setCurrentItem(1);
                    break;
                case R.id.nav_library:
                    viewPager.setCurrentItem(2);
                    break;
                case R.id.nav_profile:
                    viewPager.setCurrentItem(3);
                    break;
            }
            return true;
        });
    }

    private void updateMiniPlayer() {
        if (serviceBound && musicService != null) {
            Song currentSong = musicService.getCurrentSong();
            if (currentSong != null) {
                miniPlayerContainer.setVisibility(View.VISIBLE);
                miniPlayerTitle.setText(currentSong.getTitle());
                miniPlayerArtist.setText(currentSong.getArtistId());
                
                Glide.with(this)
                        .load(currentSong.getCoverUrl())
                        .placeholder(R.mipmap.ic_launcher)
                        .into(miniPlayerCover);

                // Update play/pause button
                if (musicService.isPlaying()) {
                    miniPlayerPlayPause.setImageResource(R.drawable.ic_pause_white_36dp);
                    startProgressUpdate();
                } else {
                    miniPlayerPlayPause.setImageResource(R.drawable.ic_play_white_36dp);
                    stopProgressUpdate();
                }

                // Update progress bar
                updateProgress();
            } else {
                miniPlayerContainer.setVisibility(View.GONE);
                stopProgressUpdate();
            }
        } else {
            miniPlayerContainer.setVisibility(View.GONE);
            stopProgressUpdate();
        }
    }

    private void startProgressUpdate() {
        progressHandler.post(progressUpdateRunnable);
    }

    private void stopProgressUpdate() {
        progressHandler.removeCallbacks(progressUpdateRunnable);
    }

    private void updateProgress() {
        if (serviceBound && musicService != null) {
            int duration = musicService.getDuration();
            int currentPosition = musicService.getCurrentPosition();
            
            if (duration > 0) {
                int progress = (int) ((currentPosition * 100.0) / duration);
                miniPlayerProgress.setProgress(progress);
            }
        }
    }

    private Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (serviceBound && musicService != null && musicService.isPlaying()) {
                updateProgress();
                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (!serviceBound) {
            Intent intent = new Intent(this, MusicService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound && !isChangingConfigurations()) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdate();
        // Hủy đăng ký BroadcastReceiver
        try {
            unregisterReceiver(musicUpdateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver đã được unregister rồi
        }
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    private void testFirestoreUtils() {
        Log.d(TAG, "Bắt đầu test FirestoreUtils");

        // Test lấy bài hát từ playlist
        String playlistId = "124e5b94-4f0f-487b-98ec-663c04e96979";
        FirestoreUtils.getSongsByPlaylistId(playlistId, new FirestoreUtils.FirestoreCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> playlistSongs) {
                StringBuilder result = new StringBuilder("=== Tất cả bài hát ===\n");
                result.append("Playlist ID: ").append(playlistId).append("\n");
                result.append("Số lượng: ").append(playlistSongs.size()).append("\n\n");

                for (Song song : playlistSongs) {
                    result.append("Bài hát: ").append(song.getTitle())
                            .append(" - ").append(song.getArtistId())
                            .append("\n");
                }

                // Log kết quả
                Log.d(TAG, result.toString());
                Toast.makeText(MainActivity.this,
                        "Test hoàn tất, xem log để biết kết quả", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting playlist songs", e);
                Toast.makeText(MainActivity.this,
                        "Lỗi playlist: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadCurrentSongFromFirebase() {
        // Kiểm tra user đã đăng nhập chưa
        if (firebaseService == null) return;
        
        firebaseService.getCurrentUserSongPlaying(new FirebaseService.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String songId) {
                if (songId != null && !songId.isEmpty()) {
                    Log.d(TAG, "Found current song ID: " + songId);
                    // Lấy thông tin bài hát từ songId
                    firebaseService.getSongById(songId, new FirebaseService.FirestoreCallback<Song>() {
                        @Override
                        public void onSuccess(Song song) {
                            Log.d(TAG, "Loaded current song: " + song.getTitle());
                            // Set bài hát vào MusicService và update mini player
                            if (serviceBound && musicService != null) {
                                List<Song> singleSongList = new ArrayList<>();
                                singleSongList.add(song);
                                musicService.setList(singleSongList, 0);
                                // Không auto play, chỉ hiển thị trong mini player
                                updateMiniPlayerWithSong(song);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading current song", e);
                        }
                    });
                } else {
                    Log.d(TAG, "No current song found");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting current song ID", e);
            }
        });
    }

    private void updateMiniPlayerWithSong(Song song) {
        if (song != null) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
            miniPlayerTitle.setText(song.getTitle());
            miniPlayerArtist.setText(song.getArtistId());
            
            Glide.with(this)
                    .load(song.getCoverUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .into(miniPlayerCover);

            // Set button to play state (not playing yet)
            miniPlayerPlayPause.setImageResource(R.drawable.ic_play_white_36dp);
            miniPlayerProgress.setProgress(0);
        }
    }

    // Method public để gọi từ bên ngoài khi cần refresh current song
    public void refreshCurrentSong() {
        loadCurrentSongFromFirebase();
    }

    // Method public để stop music service khi đăng xuất
    public void stopMusicService() {
        if (serviceBound && musicService != null) {
            musicService.stopMusic();
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
        // Stop service hoàn toàn
        Intent serviceIntent = new Intent(this, MusicService.class);
        stopService(serviceIntent);
        
        // Ẩn mini player
        miniPlayerContainer.setVisibility(View.GONE);
        stopProgressUpdate();
    }
}