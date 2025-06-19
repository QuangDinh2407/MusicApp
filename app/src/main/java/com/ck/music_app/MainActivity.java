package com.ck.music_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ck.music_app.Auth.LoginActivity;
import com.ck.music_app.MainFragment.MusicPlayerFragment;
import com.ck.music_app.MainFragment.HomeFragment;
import com.ck.music_app.MainFragment.LibraryFragment;
import com.ck.music_app.MainFragment.ProfileFragment;
import com.ck.music_app.MainFragment.SearchFragment;
import com.ck.music_app.MainFragment.HomeChildFragment.AlbumSongsFragment;
import com.ck.music_app.MainFragment.HomeChildFragment.PlaylistSongsFragment;
import com.ck.music_app.Model.Song;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.Viewpager.MainPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;
import com.ck.music_app.utils.FirestoreUtils;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ck.music_app.Services.InternetService;
import com.ck.music_app.utils.LoginUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Fragment[] fragments;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private MusicPlayerFragment musicplayerFragment;
    private List<Song> currentPlaylist;
    private int currentSongIndex;
    private Song currentSong;
    private boolean isPlaying = false;
    private FirebaseAuth auth;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private boolean isOfflineMode = false;
    private View playerContainer;
    private boolean isHandlingConnection = false;

    private final BroadcastReceiver playerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("SHOW_PLAYER".equals(intent.getAction())) {
                ArrayList<Song> songList = (ArrayList<Song>) intent.getSerializableExtra("songList");
                int position = intent.getIntExtra("position", 0);
                String albumName = intent.getStringExtra("albumName");
                showPlayer(songList, position, albumName);
            }
        }
    };

    private final BroadcastReceiver internetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(InternetService.BROADCAST_INTERNET_STATE)) {
                boolean isConnected = intent.getBooleanExtra("isConnected", false);
                handleInternetStateChange(isConnected);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase Auth và SharedPreferences
        auth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // Khởi tạo views
        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        playerContainer = findViewById(R.id.player_container);
        
        // Kiểm tra xem có đang ở chế độ offline không
        isOfflineMode = getIntent().getBooleanExtra("openDownloadFragment", false);

        // Khởi tạo các fragment chính
        fragments = new Fragment[] {
                new HomeFragment(),
                new SearchFragment(),
                new LibraryFragment(),
                new ProfileFragment()
        };

        MainPagerAdapter adapter = new MainPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);

        
        // Đảm bảo adapter được set trước khi chuyển trang
        viewPager.post(() -> {
            // Kiểm tra xem có yêu cầu mở fragment download không
            if (isOfflineMode) {
                viewPager.setCurrentItem(3, false); // Chuyển đến Profile Fragment (index 3)
                bottomNavigationView.setSelectedItemId(R.id.nav_profile); // Cập nhật bottom navigation
                // Gửi broadcast để mở fragment download
                Intent intent = new Intent("OPEN_DOWNLOAD_FRAGMENT");
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            } else {
                viewPager.setCurrentItem(0, false);
            }
        });

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

        // Đăng ký broadcast receiver
        IntentFilter filter = new IntentFilter("SHOW_PLAYER");
        LocalBroadcastManager.getInstance(this).registerReceiver(playerReceiver, filter);

        // Đăng ký broadcast receiver cho internet state
        IntentFilter internetFilter = new IntentFilter(InternetService.BROADCAST_INTERNET_STATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(internetReceiver, internetFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký tất cả broadcast receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playerReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(internetReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePlayerIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handlePlayerIntent(getIntent());
    }

    private void handlePlayerIntent(Intent intent) {
        if (intent != null) {
            if (intent.getBooleanExtra("showPlayer", false)) {
                System.out.println(intent);
                ArrayList<Song> songList = (ArrayList<Song>) intent.getSerializableExtra("songList");
                int position = intent.getIntExtra("position", 0);
                String albumName = intent.getStringExtra("albumName");
                boolean resumePlayback = intent.getBooleanExtra("resume_playback", false);
                int playbackPosition = intent.getIntExtra("playback_position", 0);

                // Hiển thị player trước để tránh delay UI
                showPlayer(songList, position, albumName, resumePlayback);

                // Xử lý phát nhạc
                Intent serviceIntent = new Intent(this, MusicService.class);
                if (resumePlayback) {
                    serviceIntent.setAction(MusicService.ACTION_RESUME);
                } else {
                    serviceIntent.setAction(MusicService.ACTION_PLAY);
                    serviceIntent.putExtra("songList", songList);
                    serviceIntent.putExtra("position", position);
                }
                startService(serviceIntent);

                if (playbackPosition > 0) {
                    // Seek đến vị trí đang phát
                    new Handler().postDelayed(() -> {
                        Intent seekIntent = new Intent(this, MusicService.class);
                        seekIntent.setAction(MusicService.ACTION_SEEK);
                        seekIntent.putExtra("position", playbackPosition);
                        startService(seekIntent);
                    }, 100);
                }

                // Xóa flag để tránh hiển thị lại player khi activity resume
                intent.removeExtra("showPlayer");
                return;
            }

            if (intent.getBooleanExtra("load_song_from_login", false)) {
                // Xử lý trường hợp load từ login
                Song currentSong = (Song) intent.getSerializableExtra("current_song");
                if (currentSong != null) {
                    ArrayList<Song> songList = new ArrayList<>();
                    songList.add(currentSong);
                    
                    // Phát nhạc
                    Intent serviceIntent = new Intent(this, MusicService.class);
                    serviceIntent.setAction(MusicService.ACTION_PLAY);
                    serviceIntent.putExtra("songList", songList);
                    serviceIntent.putExtra("position", 0);
                    startService(serviceIntent);

                    // Hiển thị player
                    showPlayer(songList, 0, "Last PLaying", true);
                }
            }
        }
    }

    public void showPlayer(List<Song> songList, int position, String albumName, boolean shouldMinimize) {
        // Lưu trạng thái hiện tại
        currentPlaylist = new ArrayList<>(songList);
        currentSongIndex = position;
        currentSong = songList.get(position);

        // Hiển thị player fragment
        if (musicplayerFragment == null) {
            musicplayerFragment = MusicPlayerFragment.newInstance(currentPlaylist, currentSongIndex, albumName, shouldMinimize);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_container, musicplayerFragment)
                    .commit();
            playerContainer.setVisibility(View.VISIBLE);

            // Gửi intent đến Service để phát nhạc
            Intent intent = new Intent(this, MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY);
            intent.putExtra("songList", new ArrayList<>(songList));
            intent.putExtra("position", position);
            startService(intent);

            if (shouldMinimize) {
                // Đợi lâu hơn để đảm bảo MediaPlayer đã được khởi tạo và bắt đầu phát
                new Handler().postDelayed(() -> {
                    Intent pauseIntent = new Intent(this, MusicService.class);
                    pauseIntent.setAction(MusicService.ACTION_PAUSE);
                    startService(pauseIntent);
                }, 2000); // Tăng delay lên 2 giây
            }
        } else {
            musicplayerFragment.updateAlbumName(albumName);
            musicplayerFragment.updatePlayerInfo(songList, position);
            if (shouldMinimize) {
                musicplayerFragment.minimize();
            } else {
                musicplayerFragment.maximize();
            }
        }
    }

    // Overload cho các trường hợp không cần minimize
    public void showPlayer(List<Song> songList, int position, String albumName) {
        showPlayer(songList, position, albumName, false);
    }

    public List<Song> getCurrentPlaylist() {
        return currentPlaylist;
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    @Override
    public void onBackPressed() {
        // Check if there are fragments in back stack
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // Show ViewPager and bottom navigation again
            viewPager.setVisibility(View.VISIBLE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            getSupportFragmentManager().popBackStack();
        } else if (musicplayerFragment != null && playerContainer.getVisibility() == View.VISIBLE) {
            musicplayerFragment.minimize();
        } else {
            super.onBackPressed();
        }
    }

    public void playSong(List<Song> songList, int position) {
        try {
            // Validate input parameters
            if (songList == null || songList.isEmpty()) {
                Log.e(TAG, "Song list is null or empty");
                Toast.makeText(this, "Danh sách bài hát trống", Toast.LENGTH_SHORT).show();
                return;
            }

            if (position < 0 || position >= songList.size()) {
                Log.e(TAG, "Invalid position: " + position + ", list size: " + songList.size());
                Toast.makeText(this, "Vị trí bài hát không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate song data
            Song selectedSong = songList.get(position);
            if (selectedSong == null) {
                Log.e(TAG, "Selected song is null");
                Toast.makeText(this, "Thông tin bài hát không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lưu danh sách bài hát và vị trí hiện tại
            this.currentPlaylist = new ArrayList<>(songList);
            this.currentSongIndex = position;
            this.currentSong = selectedSong;

            // Hiển thị player
            if (musicplayerFragment == null) {
                try {
                    musicplayerFragment = new MusicPlayerFragment();
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.player_container, musicplayerFragment)
                            .commitAllowingStateLoss(); // Use commitAllowingStateLoss to prevent IllegalStateException
                } catch (Exception e) {
                    Log.e(TAG, "Error creating MusicPlayerFragment: " + e.getMessage(), e);
                    Toast.makeText(this, "Lỗi khi tạo trình phát nhạc", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Hiển thị container player
            if (playerContainer != null) {
                playerContainer.setVisibility(View.VISIBLE);
            }

            // Cập nhật danh sách bài hát trong player (với null check)
            if (musicplayerFragment != null) {
                try {
                    musicplayerFragment.updateSongList(songList, position);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating song list in player: " + e.getMessage(), e);
                    // Don't return here, still try to start the service
                }
            }

            // Gửi intent để phát nhạc
            try {
                Intent intent = new Intent(this, MusicService.class);
                intent.setAction(MusicService.ACTION_PLAY);
                intent.putExtra("songList", new ArrayList<>(songList));
                intent.putExtra("position", position);
                startService(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting MusicService: " + e.getMessage(), e);
                Toast.makeText(this, "Lỗi khi bắt đầu phát nhạc", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in playSong: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi không mong muốn khi phát nhạc", Toast.LENGTH_SHORT).show();
        }
    }

   
    public void navigateToFragment(Fragment fragment, String backStackName) {
        try {
            // Hide the ViewPager and show fragment in fragment_container
            viewPager.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.GONE);

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_left,
                            R.anim.slide_out_left,
                            R.anim.slide_in_right,
                            R.anim.slide_out_right)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(backStackName)
                    .commit();

        } catch (Exception e) {
            Log.e(TAG, "Error navigating to fragment: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi mở trang: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleInternetStateChange(boolean isConnected) {
        if (isConnected && isOfflineMode && !isHandlingConnection) {
            isHandlingConnection = true;

            // Nếu có internet và đang ở chế độ offline
            boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER, false);
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");

            if (isRemembered && !savedEmail.isEmpty() && !savedPassword.isEmpty()) {
                // Có thông tin đăng nhập đã lưu, thử tự động đăng nhập
                auth.signInWithEmailAndPassword(savedEmail, savedPassword)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Đăng nhập thành công, sử dụng LoginUtils để xử lý
                            isOfflineMode = false;
                            LoginUtils.handleLoginSuccess(this, auth.getCurrentUser(), savedEmail);
                        } else {
                            // Đăng nhập thất bại, chuyển về màn login
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.clear();
                            editor.apply();
                            Intent intent = new Intent(this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        isHandlingConnection = false;
                    });
            } else {
                // Không có thông tin đăng nhập, chuyển về màn login
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                isHandlingConnection = false;
            }
        }
    }
}