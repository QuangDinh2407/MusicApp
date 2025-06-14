package com.ck.music_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.ck.music_app.MainFragment.HomeFragment;
import com.ck.music_app.MainFragment.LibraryFragment;
import com.ck.music_app.MainFragment.ProfileFragment;
import com.ck.music_app.MainFragment.SearchFragment;
import com.ck.music_app.Viewpager.MainPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Fragment[] fragments;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
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