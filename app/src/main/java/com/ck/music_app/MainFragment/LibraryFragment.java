package com.ck.music_app.MainFragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.ck.music_app.MainFragment.LibraryChildFragment.AlbumContentFragment;
import com.ck.music_app.MainFragment.LibraryChildFragment.ArtistContentFragment;
import com.ck.music_app.MainFragment.LibraryChildFragment.PlaylistContentFragment;
import com.ck.music_app.Model.Album;
import com.ck.music_app.R;
import com.ck.music_app.Services.FirebaseService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LibraryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LibraryFragment extends Fragment {

    private ViewPager2 viewPager;
    private ChipGroup chipGroupTabs;
    private Chip chipPlaylists, chipArtists, chipAlbums;
    private ImageButton btnAdd, btnSort;
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
                showAddAlbumsDialog();
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
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm nội dung", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedChipId = chipGroupTabs.getCheckedChipId();
            if (selectedChipId == R.id.chipPlaylists) {
                if (playlistFragment != null) {
                    playlistFragment.showAddPlaylistDialog();
                }
            } else if (selectedChipId == R.id.chipAlbums) {
                showAddAlbumsDialog();
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
                    // Gọi phương thức sắp xếp từ ArtistContentFragment
                    if (artistFragment != null && artistFragment.isAdded()) {
                        artistFragment.sortArtistsByName();
                        Toast.makeText(requireContext(), "Đã sắp xếp theo tên", Toast.LENGTH_SHORT).show();
                    }
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

    private void showAddAlbumsDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm album yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query tất cả albums từ Firestore
        db.collection("albums").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!isAdded() || getContext() == null) return;

            List<Album> allAlbums = new ArrayList<>();
            List<Album> filteredAlbums = new ArrayList<>();
            List<Album> albumsToAdd = new ArrayList<>();

            // Lấy danh sách album hiện tại của user để lọc
            db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    List<String> favoriteAlbumIds = (List<String>) userDoc.get("favoriteAlbumIds");
                    if (favoriteAlbumIds == null) {
                        favoriteAlbumIds = new ArrayList<>();
                    }

                    final List<String> currentFavoriteIds = favoriteAlbumIds;

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Album album = document.toObject(Album.class);
                        if (album != null) {
                            album.setId(document.getId());
                            // Sử dụng album ID để so sánh, tránh trùng lặp
                            if (!currentFavoriteIds.contains(album.getId())) {
                                allAlbums.add(album);
                                filteredAlbums.add(album);
                            }
                        }
                    }

                    if (allAlbums.isEmpty()) {
                        Toast.makeText(getContext(), "Tất cả album đã có trong danh sách yêu thích", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Tạo AlertDialog với ListView custom
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.LightDialogTheme);
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_albums, null);
                    TextView tvSelectedCount = dialogView.findViewById(R.id.tvSelectedCount);
                    EditText etSearchAlbums = dialogView.findViewById(R.id.etSearchAlbums);
                    ListView listView = dialogView.findViewById(R.id.listViewAlbums);

                    // Cập nhật số lượng album đã chọn
                    tvSelectedCount.setText("Đã chọn: 0");

                    // Tạo adapter cho ListView với layout tùy chỉnh
                    ArrayAdapter<Album> adapter = new ArrayAdapter<Album>(requireContext(), 0, filteredAlbums) {
                        @NonNull
                        @Override
                        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = getLayoutInflater().inflate(R.layout.item_album_select, parent, false);
                            }

                            Album album = getItem(position);
                            if (album != null) {
                                ImageView imgAlbumCover = convertView.findViewById(R.id.imgAlbumCover);
                                TextView tvAlbumTitle = convertView.findViewById(R.id.tvAlbumTitle);
                                TextView tvArtistName = convertView.findViewById(R.id.tvArtistName);
                                View selectedBackground = convertView.findViewById(R.id.selectedBackground);

                                tvAlbumTitle.setText(album.getTitle());
                                // Lấy tên artist từ artistId
                                FirebaseService.getInstance().getArtistNameById(album.getArtistId(), new FirebaseService.FirestoreCallback<String>() {
                                    @Override
                                    public void onSuccess(String artistName) {
                                        tvArtistName.setText(artistName);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        // Nếu không lấy được tên artist, hiển thị "Unknown Artist"
                                        tvArtistName.setText("Unknown Artist");
                                        Log.e("PlaylistDetail", "Error getting artist name: " + e.getMessage());
                                    }
                                });

                                // Load ảnh album
                                Glide.with(LibraryFragment.this)
                                    .load(album.getCoverUrl())
                                    .placeholder(R.drawable.love)
                                    .error(R.drawable.love)
                                    .into(imgAlbumCover);

                                // Hiển thị background mờ nếu item được chọn
                                selectedBackground.setVisibility(albumsToAdd.contains(album) ? View.VISIBLE : View.GONE);
                            }

                            return convertView;
                        }
                    };

                    listView.setAdapter(adapter);

                    // Xử lý sự kiện khi người dùng chọn/bỏ chọn album
                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        Album album = filteredAlbums.get(position);
                        View selectedBackground = view.findViewById(R.id.selectedBackground);

                        if (albumsToAdd.contains(album)) {
                            albumsToAdd.remove(album);
                            selectedBackground.setVisibility(View.GONE);
                        } else {
                            albumsToAdd.add(album);
                            selectedBackground.setVisibility(View.VISIBLE);
                        }

                        // Cập nhật số lượng album đã chọn
                        tvSelectedCount.setText("Đã chọn: " + albumsToAdd.size());
                    });

                    // Xử lý sự kiện tìm kiếm
                    etSearchAlbums.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            String searchText = s.toString().toLowerCase().trim();
                            filteredAlbums.clear();

                            if (searchText.isEmpty()) {
                                filteredAlbums.addAll(allAlbums);
                            } else {
                                for (Album album : allAlbums) {
                                    if (album.getTitle().toLowerCase().contains(searchText) ||
                                        album.getArtistId().toLowerCase().contains(searchText)) {
                                        filteredAlbums.add(album);
                                    }
                                }
                            }

                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void afterTextChanged(Editable s) {}
                    });

                    builder.setView(dialogView);

                    builder.setPositiveButton("Thêm", (dialog, which) -> {
                        if (!albumsToAdd.isEmpty()) {
                            addAlbumsToFavorites(albumsToAdd);
                        }
                    });

                    builder.setNegativeButton("Hủy", null);

                    AlertDialog dialog = builder.create();
                    dialog.show();
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi khi tải danh sách album yêu thích", Toast.LENGTH_SHORT).show();
                    }
                });
        }).addOnFailureListener(e -> {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi tải danh sách album", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addAlbumsToFavorites(List<Album> albumsToAdd) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || !isAdded() || getContext() == null) return;

        List<String> albumIdsToAdd = new ArrayList<>();
        List<String> albumNamesToAdd = new ArrayList<>();
        for (Album album : albumsToAdd) {
            albumIdsToAdd.add(album.getId());
            albumNamesToAdd.add(album.getTitle());
        }

        // Cập nhật Firestore với cả ID và names để tương thích ngược
        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
            .update(
                "favoriteAlbumIds", FieldValue.arrayUnion(albumIdsToAdd.toArray()),
                "favoriteAlbums", FieldValue.arrayUnion(albumNamesToAdd.toArray())
            )
            .addOnSuccessListener(aVoid -> {
                if (!isAdded() || getContext() == null) return;

                // Gửi broadcast để cập nhật AlbumContentFragment
                for (Album album : albumsToAdd) {
                    Intent intent = new Intent("favorite_album_updated");
                    intent.putExtra("albumName", album.getTitle());
                    intent.putExtra("albumId", album.getId());
                    intent.putExtra("isFavorite", true);
                    intent.putExtra("coverUrl", album.getCoverUrl());
                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
                }

                Toast.makeText(getContext(),
                    "Đã thêm " + albumsToAdd.size() + " album vào danh sách yêu thích",
                    Toast.LENGTH_SHORT).show();

                // Refresh album fragment if it's currently visible
                if (viewPager.getCurrentItem() == 2 && albumFragment != null) {
                    albumFragment.onResume();
                }
            })
            .addOnFailureListener(e -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(),
                        "Lỗi khi thêm album: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
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