package com.ck.music_app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.ck.music_app.Fragment.MiniPlayerFragment;
import com.ck.music_app.Fragment.PlayMusicFragment;
import com.ck.music_app.MainFragment.HomeFragment;
import com.ck.music_app.MainFragment.LibraryFragment;
import com.ck.music_app.MainFragment.ProfileFragment;
import com.ck.music_app.MainFragment.SearchFragment;
import com.ck.music_app.Model.Song;
import com.ck.music_app.Viewpager.MainPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Lấy email từ Intent và hiển thị Toast
        String email = getIntent().getStringExtra("email");
        if (email != null && !email.isEmpty()) {
            Toast.makeText(this, "Xin chào: " + email, Toast.LENGTH_LONG).show();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Chạy test ngay khi khởi tạo
        testFirestoreUtils();

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
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        playerContainer = findViewById(R.id.player_container);

        // Khởi tạo mini player
        miniPlayerFragment = new MiniPlayerFragment();
        miniPlayerFragment.setOnMiniPlayerClickListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mini_player_container, miniPlayerFragment)
                .commit();

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
    }
}