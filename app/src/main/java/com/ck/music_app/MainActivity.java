package com.ck.music_app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ck.music_app.MainFragment.HomeFragment;
import com.ck.music_app.MainFragment.LibraryFragment;
import com.ck.music_app.MainFragment.ProfileFragment;
import com.ck.music_app.MainFragment.SearchFragment;
import com.ck.music_app.Viewpager.MainPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.ck.music_app.Model.Song;
import com.ck.music_app.utils.FirestoreUtils;
import java.util.List;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Fragment[] fragments;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;

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
        // Chạy test ngay khi khởi tạo
        testFirestoreUtils();

        // Khởi tạo các fragment chỉ một lần
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

}