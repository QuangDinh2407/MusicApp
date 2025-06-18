package com.ck.music_app.MainFragment.LibraryChildFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.Model.Album;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import com.ck.music_app.Adapter.AlbumAdapter;
import com.ck.music_app.MainFragment.HomeChildFragment.AlbumSongsFragment;
import com.ck.music_app.utils.FirestoreUtils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AlbumContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlbumContentFragment extends Fragment {

    private static final String BROADCAST_FAVORITE_UPDATED = "favorite_album_updated";
    
    private RecyclerView rvAlbum;
    private LinearLayout layoutEmptyState;
    private ChipGroup chipGroupFilter;
    private Chip chipRecent, chipAlphabetical;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private List<Album> favoriteAlbums = new ArrayList<>();
    private AlbumAdapter albumAdapter;

    private final BroadcastReceiver favoriteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BROADCAST_FAVORITE_UPDATED.equals(intent.getAction())) {
                String albumName = intent.getStringExtra("albumName");
                boolean isFavorite = intent.getBooleanExtra("isFavorite", false);
                String coverUrl = intent.getStringExtra("coverUrl");
                
                if (isFavorite) {
                    // Tải thông tin album từ Firestore
                    db.collection("albums")
                        .whereEqualTo("title", albumName)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                Album album = doc.toObject(Album.class);
                                if (album != null) {
                                    album.setId(doc.getId());
                                    // Kiểm tra xem album đã tồn tại trong danh sách chưa
                                    boolean exists = false;
                                    for (Album existingAlbum : favoriteAlbums) {
                                        if (existingAlbum.getTitle().equals(albumName)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        favoriteAlbums.add(album);
                                        updateAlbumList();
                                    }
                                }
                            }
                        });
                } else {
                    // Xóa album khỏi danh sách yêu thích
                    favoriteAlbums.removeIf(album -> album.getTitle().equals(albumName));
                    updateAlbumList();
                }
            }
        }
    };

    public AlbumContentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment AlbumContentFragment.
     */
    public static AlbumContentFragment newInstance() {
        return new AlbumContentFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Register broadcast receiver only if context is available
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter(BROADCAST_FAVORITE_UPDATED);
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(favoriteUpdateReceiver, filter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister broadcast receiver safely
        if (getContext() != null) {
            try {
                LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(favoriteUpdateReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered, ignore
            }
        }
        // Cleanup data
        if (favoriteAlbums != null) {
            favoriteAlbums.clear();
        }
        db = null;
        currentUser = null;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cleanup views và adapter để tránh crash khi theme thay đổi
        if (rvAlbum != null) {
            rvAlbum.setAdapter(null);
        }
        albumAdapter = null;
        rvAlbum = null;
        layoutEmptyState = null;
        chipGroupFilter = null;
        chipRecent = null;
        chipAlphabetical = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_content, container, false);
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        return view;
    }

    private void initializeViews(View view) {
        rvAlbum = view.findViewById(R.id.rvAlbum);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipRecent = view.findViewById(R.id.chipRecent);
        chipAlphabetical = view.findViewById(R.id.chipAlphabetical);

        // Set default selection
        chipRecent.setChecked(true);
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        
        int spanCount = 2; // Number of columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        rvAlbum.setLayoutManager(layoutManager);
        
        albumAdapter = new AlbumAdapter(requireContext(), favoriteAlbums);
        albumAdapter.setOnAlbumClickListener(this::showAlbumSongs);
        rvAlbum.setAdapter(albumAdapter);
        
        // Load favorite albums
        loadFavoriteAlbums();
    }

    private void loadFavoriteAlbums() {
        // Kiểm tra currentUser và fragment state
        if (currentUser == null) {
            currentUser = FirebaseAuth.getInstance().getCurrentUser();
        }
        
        if (currentUser == null || !isAdded() || getContext() == null) {
            showEmptyState(true);
            return;
        }

        // Xóa danh sách cũ
        favoriteAlbums.clear();
        if (albumAdapter != null) {
            albumAdapter.notifyDataSetChanged();
        }

        db.collection("users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!isAdded() || getContext() == null) return;
                
                List<String> favoriteAlbumNames = (List<String>) documentSnapshot.get("favoriteAlbums");
                if (favoriteAlbumNames != null && !favoriteAlbumNames.isEmpty()) {
                    // Load each album's details
                    for (String albumName : favoriteAlbumNames) {
                        db.collection("albums")
                            .whereEqualTo("title", albumName)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!isAdded() || getContext() == null) return;
                                
                                if (!querySnapshot.isEmpty()) {
                                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                    Album album = doc.toObject(Album.class);
                                    if (album != null) {
                                        album.setId(doc.getId());
                                        favoriteAlbums.add(album);
                                        updateAlbumList();
                                    }
                                }
                            });
                    }
                } else {
                    showEmptyState(true);
                }
            })
            .addOnFailureListener(e -> {
                if (!isAdded() || getContext() == null) return;
                
                Toast.makeText(requireContext(),
                    "Lỗi khi tải danh sách album yêu thích",
                    Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            });
    }

    private void updateAlbumList() {
        if (!isAdded() || getContext() == null) return;
        
        if (favoriteAlbums.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            if (albumAdapter != null && chipGroupFilter != null) {
                // Áp dụng sắp xếp dựa trên chip đang được chọn
                int checkedId = chipGroupFilter.getCheckedChipId();
                if (checkedId == R.id.chipAlphabetical) {
                    favoriteAlbums.sort((a1, a2) -> {
                        if (a1.getTitle() == null) return 1;
                        if (a2.getTitle() == null) return -1;
                        return a1.getTitle().compareToIgnoreCase(a2.getTitle());
                    });
                }
                // Nếu là chipRecent thì giữ nguyên thứ tự từ Firestore
                albumAdapter.notifyDataSetChanged();
            }
        }
    }

    private void setupListeners() {
        if (chipGroupFilter == null) return;
        
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isAdded() || getContext() == null) return;
            
            // Kiểm tra currentUser trước khi sử dụng
            if (currentUser == null) {
                currentUser = FirebaseAuth.getInstance().getCurrentUser();
            }
            
            if (currentUser == null) return;
            
            if (checkedId == R.id.chipRecent) {
                // Sắp xếp theo thời gian thêm vào gần đây nhất
                db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!isAdded() || getContext() == null || albumAdapter == null) return;
                        
                        List<String> favoriteAlbumNames = (List<String>) documentSnapshot.get("favoriteAlbums");
                        if (favoriteAlbumNames != null) {
                            // Sắp xếp album theo thứ tự trong favoriteAlbums (thứ tự thêm vào)
                            favoriteAlbums.sort((a1, a2) -> {
                                int index1 = favoriteAlbumNames.indexOf(a1.getTitle());
                                int index2 = favoriteAlbumNames.indexOf(a2.getTitle());
                                return Integer.compare(index2, index1); // Đảo ngược để mới nhất lên đầu
                            });
                            albumAdapter.notifyDataSetChanged();
                        }
                    });
            } else if (checkedId == R.id.chipAlphabetical) {
                // Sắp xếp theo tên A-Z
                favoriteAlbums.sort((a1, a2) -> {
                    if (a1.getTitle() == null) return 1;
                    if (a2.getTitle() == null) return -1;
                    return a1.getTitle().compareToIgnoreCase(a2.getTitle());
                });
                if (albumAdapter != null) {
                    albumAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvAlbum.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showAlbumSongs(Album album) {
        if (!isAdded() || getContext() == null) return;
        
        // Load songs for this album
        FirestoreUtils.getSongsByAlbumId(album.getId(), new FirestoreUtils.FirestoreCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> songs) {
                if (!isAdded() || getContext() == null) return;
                
                // Create and show AlbumSongsFragment
                AlbumSongsFragment albumSongsFragment = AlbumSongsFragment.newInstance(
                    songs,
                    album.getTitle(),
                    album.getCoverUrl()
                );

                // Add callback để xử lý khi fragment bị remove
                albumSongsFragment.setOnFragmentDismissListener(() -> {
                    // Hiển thị lại RecyclerView và ẩn container khi fragment bị đóng
                    rvAlbum.setVisibility(View.VISIBLE);
                    View container = requireView().findViewById(R.id.albumSongsContainer);
                    container.setVisibility(View.GONE);
                });

                // Ẩn RecyclerView và hiển thị container
                rvAlbum.setVisibility(View.GONE);
                View container = requireView().findViewById(R.id.albumSongsContainer);
                container.setVisibility(View.VISIBLE);

                // Thực hiện transaction để thêm fragment mới
                if (getChildFragmentManager() != null) {
                    getChildFragmentManager().beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_right,
                            R.anim.slide_in_right,
                            R.anim.slide_out_right
                        )
                        .replace(R.id.albumSongsContainer, albumSongsFragment)
                        .addToBackStack(null)
                        .commit();
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded() || getContext() == null) return;
                
                Toast.makeText(requireContext(),
                    "Lỗi khi tải danh sách bài hát: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại danh sách album yêu thích mỗi khi fragment được hiển thị
        loadFavoriteAlbums();
    }
} 