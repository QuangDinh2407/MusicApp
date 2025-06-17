package com.ck.music_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ck.music_app.MainFragment.MusicPlayerFragment;
import com.ck.music_app.MainFragment.HomeFragment;
import com.ck.music_app.MainFragment.LibraryFragment;
import com.ck.music_app.MainFragment.ProfileFragment;
import com.ck.music_app.MainFragment.SearchFragment;
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
import java.util.List;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

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
    private FirebaseFirestore db;
    private View playerContainer;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Khởi tạo các fragment chính
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
        playerContainer = findViewById(R.id.player_container);

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playerReceiver);
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
        if (intent != null && intent.getBooleanExtra("showPlayer", false)) {
            ArrayList<Song> songList = (ArrayList<Song>) intent.getSerializableExtra("songList");
            int position = intent.getIntExtra("position", 0);
            String albumName = intent.getStringExtra("albumName");
            
            // Phát nhạc
            Intent serviceIntent = new Intent(this, MusicService.class);
            serviceIntent.setAction(MusicService.ACTION_PLAY);
            serviceIntent.putExtra("songList", songList);
            serviceIntent.putExtra("position", position);
            startService(serviceIntent);

            // Hiển thị player
            showPlayer(songList, position, albumName);
            
            // Xóa flag để tránh hiển thị lại player khi activity resume
            intent.removeExtra("showPlayer");
        }
    }

    public void showPlayer(List<Song> songList, int position, String albumName) {
        // Lưu trạng thái hiện tại
        currentPlaylist = new ArrayList<>(songList);
        currentSongIndex = position;
        currentSong = songList.get(position);

        // Gửi intent đến Service để phát nhạc
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra("songList", new ArrayList<>(songList));
        intent.putExtra("position", position);
        startService(intent);

        // Hiển thị player fragment
        if (musicplayerFragment == null) {
            musicplayerFragment = MusicPlayerFragment.newInstance(currentPlaylist, currentSongIndex, albumName);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.player_container, musicplayerFragment)
                    .commit();
            playerContainer.setVisibility(View.VISIBLE);
        } else {
            musicplayerFragment.updateAlbumName(albumName);
            musicplayerFragment.updatePlayerInfo(songList, position);
            musicplayerFragment.maximize();
        }
    }

    // Overload cho các trường hợp không có albumName
    public void showPlayer(List<Song> songList, int position) {
        showPlayer(songList, position, "Now Playing");
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
        if (musicplayerFragment != null && playerContainer.getVisibility() == View.VISIBLE) {
            musicplayerFragment.minimize();
        } else {
            super.onBackPressed();
        }
    }
}