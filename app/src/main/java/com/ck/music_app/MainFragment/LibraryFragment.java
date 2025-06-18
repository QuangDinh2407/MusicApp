package com.ck.music_app.MainFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
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
    private ImageButton btnSearch, btnAdd, btnSort;
    private ShapeableImageView ivUserAvatar;
    private FirebaseAuth mAuth;
    private LibraryPagerAdapter pagerAdapter;
    
    // Xóa static - tạo mới mỗi lần để tránh crash khi theme thay đổi
    private PlaylistContentFragment playlistFragment;
    private ArtistContentFragment artistFragment;
    private AlbumContentFragment albumFragment;

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
        // Tạo fragments mới mỗi lần onCreate để tránh giữ context cũ
        createChildFragments();
    }
    
    private void createChildFragments() {
        // Tạo fragments mới, không static
        playlistFragment = PlaylistContentFragment.newInstance();
        artistFragment = ArtistContentFragment.newInstance();
        albumFragment = AlbumContentFragment.newInstance();
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
        setupHeaderActions();
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
        btnSort = view.findViewById(R.id.btnSort);
        ivUserAvatar = view.findViewById(R.id.imgAvatar);
    }

    private void setupViewPager() {
        pagerAdapter = new LibraryPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(2); // Giữ các fragment trong bộ nhớ
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateChipSelection(position);
                updateAddButtonVisibility(position);
                updateCurrentFragment(position);
            }
        });
    }

    private void updateCurrentFragment(int position) {
        Fragment currentFragment = null;
        switch (position) {
            case 0:
                currentFragment = playlistFragment;
                break;
            case 1:
                currentFragment = artistFragment;
                break;
            case 2:
                currentFragment = albumFragment;
                break;
        }

        // Chỉ refresh khi fragment đã sẵn sàng và được attach
        if (currentFragment != null && currentFragment.isAdded() && 
            currentFragment.getContext() != null) {
            
            if (currentFragment instanceof PlaylistContentFragment) {
                ((PlaylistContentFragment) currentFragment).refreshPlaylists();
            }
            // Có thể thêm refresh cho ArtistContentFragment và AlbumContentFragment nếu cần
        }
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
            if (currentPosition == 0) {
                if (playlistFragment != null) {
                    playlistFragment.showAddPlaylistDialog();
                }
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

    private void setupHeaderActions() {
        // Avatar click
        ivUserAvatar.setOnClickListener(v -> {
            // TODO: Navigate to profile
        });

        // Search click
        btnSearch.setOnClickListener(v -> {
            // TODO: Navigate to search
        });

        // Add button visibility based on selected tab
        chipGroupTabs.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPlaylists || checkedId == R.id.chipAlbums) {
                btnAdd.setVisibility(View.VISIBLE);
                btnSort.setVisibility(View.GONE);
            } else if (checkedId == R.id.chipArtists) {
                btnAdd.setVisibility(View.GONE);
                btnSort.setVisibility(View.VISIBLE);
            } else {
                btnAdd.setVisibility(View.GONE);
                btnSort.setVisibility(View.GONE);
            }
        });

        // Add button click
        btnAdd.setOnClickListener(v -> {
            int selectedChipId = chipGroupTabs.getCheckedChipId();
            if (selectedChipId == R.id.chipPlaylists) {
                if (playlistFragment != null) {
                    playlistFragment.showAddPlaylistDialog();
                }
            } else if (selectedChipId == R.id.chipAlbums) {
                // TODO: Add new album
                Toast.makeText(requireContext(), "Thêm album mới", Toast.LENGTH_SHORT).show();
            }
        });

        // Sort button click
        btnSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), btnSort);
            popup.getMenuInflater().inflate(R.menu.menu_artist_sort, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.sort_recent) {
                    // TODO: Sort by recent
                    Toast.makeText(requireContext(), "Sắp xếp theo nghe gần đây", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.sort_name) {
                    // TODO: Sort by name
                    Toast.makeText(requireContext(), "Sắp xếp theo tên", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.sort_most_played) {
                    // TODO: Sort by most played
                    Toast.makeText(requireContext(), "Sắp xếp theo lượt nghe", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    // Adapter for ViewPager2 - Xóa static
    private class LibraryPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public LibraryPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Tạo fragments mới nếu null, đảm bảo không dùng fragments cũ
            switch (position) {
                case 0:
                    if (playlistFragment == null) {
                        playlistFragment = PlaylistContentFragment.newInstance();
                    }
                    return playlistFragment;
                case 1:
                    if (artistFragment == null) {
                        artistFragment = ArtistContentFragment.newInstance();
                    }
                    return artistFragment;
                case 2:
                    if (albumFragment == null) {
                        albumFragment = AlbumContentFragment.newInstance();
                    }
                    return albumFragment;
                default:
                    return PlaylistContentFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Number of tabs (Playlist, Nghệ sĩ, Album)
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Chỉ update khi ViewPager đã được khởi tạo
        if (viewPager != null) {
            updateCurrentFragment(viewPager.getCurrentItem());
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cleanup để tránh memory leak
        if (viewPager != null) {
            viewPager.setAdapter(null);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup fragments
        playlistFragment = null;
        artistFragment = null;
        albumFragment = null;
    }
}