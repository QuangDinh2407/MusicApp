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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.ck.music_app.Fragment.MiniPlayerFragment;
import com.ck.music_app.Fragment.PlayMusicFragment;
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
import com.ck.music_app.Viewpager.MainPagerAdapter;
import com.ck.music_app.utils.FirestoreUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;
import com.ck.music_app.utils.FirestoreUtils;
import java.util.List;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MiniPlayerFragment.OnMiniPlayerClickListener {
    private static final String TAG = "MainActivity";
    private Fragment[] fragments;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private MiniPlayerFragment miniPlayerFragment;
    private Fragment currentPlayerFragment;
    private Song currentSong;
    private List<Song> currentPlaylist;
    private int currentSongIndex = 0;
    private boolean isPlaying = false;
    private FirebaseFirestore db;
    private View miniPlayerContainer;
    private View playerContainer;

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Thêm listener để theo dõi thay đổi back stack
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            View fragmentContainer = findViewById(R.id.fragment_container);
            View viewPager = findViewById(R.id.view_pager);
            
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                // Nếu không còn fragment nào trong back stack
                if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
                if (viewPager != null) viewPager.setVisibility(View.VISIBLE);
            }
        });

        // Lấy email từ Intent và hiển thị Toast
        String email = getIntent().getStringExtra("email");
        if (email != null && !email.isEmpty()) {
            Toast.makeText(this, "Xin chào: " + email, Toast.LENGTH_LONG).show();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        firebaseService = FirebaseService.getInstance();
        testFirestoreUtils();

        // Khởi tạo các fragment chính
        fragments = new Fragment[]{
                new HomeFragment(),
                new SearchFragment(),
                new LibraryFragment(),
                new ProfileFragment()
        };
        // Kiểm tra Google Play Services trong background thread
        checkGooglePlayServicesAsync();

        // Kiểm tra và yêu cầu quyền nếu cần
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            checkAndRequestPermissions();
        }

        // Setup UI components first
        setupViews();

        // Initialize fragments lazily
        initializeFragmentsLazy();
    }

    private void setupViews() {
        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (viewPager == null || bottomNavigationView == null) {
            Log.e("MainActivity", "Failed to find views in layout");
            return;
        }

        // Setup ViewPager with empty adapter first
        MainPagerAdapter adapter = new MainPagerAdapter(this, new Fragment[4]);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0, false);
        viewPager.setOffscreenPageLimit(2); // Giới hạn số lượng fragment được giữ trong bộ nhớ

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        playerContainer = findViewById(R.id.player_container);

        // Khởi tạo mini player
        miniPlayerFragment = new MiniPlayerFragment();
        miniPlayerFragment.setOnMiniPlayerClickListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mini_player_container, miniPlayerFragment)
                .commit();

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
        setupViewPagerCallbacks();
        setupBottomNavigation();
    }

    private void initializeFragmentsLazy() {
        // Initialize fragments in background to avoid blocking main thread
        new Thread(() -> {
            fragments = new Fragment[] {
                    new HomeFragment(),
                    new SearchFragment(),
                    new LibraryFragment(),
                    new ProfileFragment()
            };

            // Update adapter on main thread
            runOnUiThread(() -> {
                if (viewPager != null && viewPager.getAdapter() instanceof MainPagerAdapter) {
                    ((MainPagerAdapter) viewPager.getAdapter()).updateFragments(fragments);
                }
            });
        }).start();
    }

    private void setupViewPagerCallbacks() {
        // Khi vuốt ViewPager2 thì đổi tab menu
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (bottomNavigationView != null && bottomNavigationView.getMenu().size() > position) {
                    bottomNavigationView.getMenu().getItem(position).setChecked(true);
                }
            }
        });
    }

    private void setupBottomNavigation() {
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

    @Override
    public void onBackPressed() {
        View fragmentContainer = findViewById(R.id.fragment_container);
        View viewPager = findViewById(R.id.view_pager);

        if (fragmentContainer != null && fragmentContainer.getVisibility() == View.VISIBLE) {
            // Nếu đang hiển thị fragment container
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                // Nếu còn fragment trong back stack
                getSupportFragmentManager().popBackStack();
            } else {
                // Nếu không còn fragment nào, ẩn container và hiện ViewPager
                fragmentContainer.setVisibility(View.GONE);
                if (viewPager != null) viewPager.setVisibility(View.VISIBLE);
            }
        } else {
            // Nếu đang ở ViewPager, xử lý back như bình thường
            super.onBackPressed();
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

    public void showMiniPlayer(Song song) {
        currentSong = song;
        // Nếu playlist chưa được khởi tạo, tạo mới với bài hát hiện tại
        if (currentPlaylist == null) {
            currentPlaylist = new ArrayList<>();
        }
        // Nếu bài hát chưa có trong playlist, thêm vào
        if (!currentPlaylist.contains(song)) {
            currentPlaylist.add(song);
            currentSongIndex = currentPlaylist.size() - 1;
        } else {
            currentSongIndex = currentPlaylist.indexOf(song);
        }
        
        miniPlayerFragment.updateSong(song);
        miniPlayerFragment.updatePlayingState(true);

        // Animation cho mini player
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        miniPlayerContainer.startAnimation(slideUp);
        miniPlayerContainer.setVisibility(View.VISIBLE);
        playerContainer.setVisibility(View.GONE);
    }

    public void showFullPlayer() {
        if (currentSong != null && currentPlaylist != null) {
            showFullPlayer(currentPlaylist, currentSongIndex);
        }
    }

    public void showFullPlayer(List<Song> songList, int position) {
        currentPlaylist = new ArrayList<>(songList);
        currentSongIndex = position;
        currentSong = songList.get(position);

        // Tạo và hiển thị full player fragment với playlist và index
        PlayMusicFragment playerFragment = PlayMusicFragment.newInstance(currentPlaylist, currentSongIndex);
        getSupportFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.player_container, playerFragment)
                .addToBackStack(null)
                .commit();

        // Animation cho full player
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        playerContainer.startAnimation(slideUp);
        playerContainer.setVisibility(View.VISIBLE);
        miniPlayerContainer.setVisibility(View.GONE);
        currentPlayerFragment = playerFragment;
    }

    public void minimizePlayer() {
        if (currentPlayerFragment != null) {
            // Animation cho việc ẩn full player
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    playerContainer.setVisibility(View.GONE);
                    // Chỉ remove PlayMusicFragment, không pop back stack
                    getSupportFragmentManager()
                        .beginTransaction()
                        .remove(currentPlayerFragment)
                        .commit();
                    
                    // Hiện mini player với animation
                    Animation slideUp = AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_up);
                    miniPlayerContainer.startAnimation(slideUp);
                    miniPlayerContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            playerContainer.startAnimation(slideDown);
        }
    }

    @Override
    public void onMiniPlayerClicked() {
        showFullPlayer();
    }

    @Override
    public void onPlayPauseClicked() {
        isPlaying = !isPlaying;
        miniPlayerFragment.updatePlayingState(isPlaying);
        if (currentPlayerFragment instanceof PlayMusicFragment) {
            ((PlayMusicFragment) currentPlayerFragment).updatePlayingState(isPlaying);
        }
    }

    @Override
    public void onBackPressed() {
        if (playerContainer.getVisibility() == View.VISIBLE) {
            minimizePlayer();
        } else {
            super.onBackPressed();
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
    private void checkGooglePlayServicesAsync() {
        // Run Google Play Services check in background thread
        new Thread(() -> {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int status = googleApiAvailability.isGooglePlayServicesAvailable(this);

            runOnUiThread(() -> {
                if (status != ConnectionResult.SUCCESS) {
                    Log.w("MainActivity", "Google Play Services not available: " + status);
                    if (googleApiAvailability.isUserResolvableError(status)) {
                        googleApiAvailability.getErrorDialog(this, status, 2404).show();
                    } else {
                        Log.e("MainActivity", "This device is not supported for Google Play Services");
                        Toast.makeText(this, "Thiết bị không hỗ trợ Google Play Services", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d("MainActivity", "Google Play Services is available");
                }
            });
        }).start();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            List<String> permissionsToRequest = new ArrayList<>();

            for (String permission : REQUIRED_PERMISSIONS) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                requestPermissions(
                        permissionsToRequest.toArray(new String[0]),
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                // Hiển thị thông báo nếu người dùng từ chối quyền
                Toast.makeText(this,
                        "Cần cấp quyền để phát nhạc khi ứng dụng chạy nền",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}