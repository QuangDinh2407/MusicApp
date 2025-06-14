package com.ck.music_app.MainFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.ck.music_app.MainFragment.LibraryChildFragment.AlbumContentFragment;
import com.ck.music_app.MainFragment.LibraryChildFragment.ArtistContentFragment;
import com.ck.music_app.MainFragment.LibraryChildFragment.PlaylistContentFragment;
import com.ck.music_app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LibraryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LibraryFragment extends Fragment {

    private ViewPager2 viewPager;
    private ChipGroup chipGroupTabs;
    private Chip chipPlaylists, chipArtists, chipAlbums;
    private ImageButton btnSearch, btnAdd;
    private ShapeableImageView ivUserAvatar;
    private FirebaseAuth mAuth;

    public LibraryFragment() {
        // Required empty public constructor
    }

    public static LibraryFragment newInstance() {
        return new LibraryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        initializeViews(view);
        setupViewPager();
        setupChips();
        setupButtons();
        updateUserAvatar();
        return view;
    }

    private void initializeViews(View view) {
        viewPager = view.findViewById(R.id.viewPagerLibrary);
        chipGroupTabs = view.findViewById(R.id.chipGroupTabs);
        chipPlaylists = view.findViewById(R.id.chipPlaylists);
        chipArtists = view.findViewById(R.id.chipArtists);
        chipAlbums = view.findViewById(R.id.chipAlbums);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnAdd = view.findViewById(R.id.btnAdd);
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
    }

    private void setupViewPager() {
        LibraryPagerAdapter pagerAdapter = new LibraryPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateChipSelection(position);
                updateAddButtonVisibility(position);
            }
        });
    }

    private void setupChips() {
        chipPlaylists.setChecked(true);
        
        View.OnClickListener chipClickListener = v -> {
            int position = 0;
            if (v.getId() == R.id.chipArtists) {
                position = 1;
            } else if (v.getId() == R.id.chipAlbums) {
                position = 2;
            }
            viewPager.setCurrentItem(position, true);
        };

        chipPlaylists.setOnClickListener(chipClickListener);
        chipArtists.setOnClickListener(chipClickListener);
        chipAlbums.setOnClickListener(chipClickListener);
    }

    private void updateChipSelection(int position) {
        chipPlaylists.setChecked(position == 0);
        chipArtists.setChecked(position == 1);
        chipAlbums.setChecked(position == 2);
    }

    private void updateAddButtonVisibility(int position) {
        // Chỉ hiển thị nút Add cho Playlists và Albums
        btnAdd.setVisibility(position == 1 ? View.GONE : View.VISIBLE);
    }

    private void setupButtons() {
        btnSearch.setOnClickListener(v -> {
            // TODO: Implement search functionality
            Toast.makeText(getContext(), "Tính năng tìm kiếm sẽ được triển khai sau", Toast.LENGTH_SHORT).show();
        });

        btnAdd.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm nội dung", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentPosition = viewPager.getCurrentItem();
            Fragment currentFragment = getChildFragmentManager()
                    .findFragmentByTag("f" + currentPosition);

            if (currentPosition == 0 && currentFragment instanceof PlaylistContentFragment) {
                ((PlaylistContentFragment) currentFragment).showAddPlaylistDialog();
            } else if (currentPosition == 2) {
                Toast.makeText(getContext(), "Chức năng thêm Album sẽ được triển khai sau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserAvatar() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivUserAvatar);
        }
    }

    // Adapter for ViewPager2
    private static class LibraryPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public LibraryPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return PlaylistContentFragment.newInstance();
                case 1:
                    return ArtistContentFragment.newInstance();
                case 2:
                    return AlbumContentFragment.newInstance();
                default:
                    return PlaylistContentFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Number of tabs (Playlist, Nghệ sĩ, Album)
        }
    }
}