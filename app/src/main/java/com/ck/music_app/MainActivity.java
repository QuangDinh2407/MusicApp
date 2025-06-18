package com.ck.music_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Khởi tạo các fragment chính
        fragments = new Fragment[] {
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

    /**
     * Method to navigate to different fragments from search results
     */
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
}