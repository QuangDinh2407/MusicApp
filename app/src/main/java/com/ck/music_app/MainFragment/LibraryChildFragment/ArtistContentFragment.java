package com.ck.music_app.MainFragment.LibraryChildFragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.Adapter.ArtistAdapter;
import com.ck.music_app.Model.Artist;
import com.ck.music_app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ArtistContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ArtistContentFragment extends Fragment implements ArtistAdapter.OnArtistClickListener {

    private RecyclerView rvFollowedArtists;
    private LinearLayout layoutEmptyState;
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipFollowing;
    private EditText etSearchArtist;
    private ImageView btnClearSearch;
    private View rootLayout;
    
    private List<Artist> allArtists = new ArrayList<>();
    private List<Artist> followedArtists = new ArrayList<>();
    private List<Artist> filteredArtists = new ArrayList<>();
    private ArtistAdapter artistAdapter;
    private FirebaseFirestore db;
    private String currentUserId;

    public ArtistContentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment ArtistContentFragment.
     */
    public static ArtistContentFragment newInstance() {
        return new ArtistContentFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_content, container, false);
        initializeViews(view);
        setupFirebase();
        setupRecyclerView();
        setupListeners();
        loadArtists();
        return view;
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }
    }

    private void initializeViews(View view) {
        rvFollowedArtists = view.findViewById(R.id.rvFollowedArtists);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipAll = view.findViewById(R.id.chipAll);
        chipFollowing = view.findViewById(R.id.chipFollowing);
        etSearchArtist = view.findViewById(R.id.etSearchArtist);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        rootLayout = view.findViewById(R.id.rootLayout);

        // Set default selection
        chipAll.setChecked(true);
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        artistAdapter = new ArtistAdapter(new ArrayList<>(), this);
        rvFollowedArtists.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFollowedArtists.setAdapter(artistAdapter);
    }

    private void setupListeners() {
        // Search functionality
        etSearchArtist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterArtists(s.toString());
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle IME action done
        etSearchArtist.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearSearchFocus();
                return true;
            }
            return false;
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchArtist.setText("");
            btnClearSearch.setVisibility(View.GONE);
            clearSearchFocus();
        });

        // Clear focus when clicking outside
        rootLayout.setOnClickListener(v -> clearSearchFocus());
        
        // Clear focus when scrolling RecyclerView
        rvFollowedArtists.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    clearSearchFocus();
                }
            }
        });

        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                filterArtists(etSearchArtist.getText().toString());
            } else if (checkedId == R.id.chipFollowing) {
                filterArtists(etSearchArtist.getText().toString());
            }
            clearSearchFocus();
        });
    }

    private void clearSearchFocus() {
        if (etSearchArtist != null) {
            etSearchArtist.clearFocus();
            // Hide keyboard
            if (getActivity() != null && getActivity().getCurrentFocus() != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
            }
        }
    }

    private void filterArtists(String query) {
        List<Artist> baseList = chipAll.isChecked() ? allArtists : followedArtists;
        
        if (query.isEmpty()) {
            filteredArtists = new ArrayList<>(baseList);
        } else {
            String lowercaseQuery = query.toLowerCase();
            filteredArtists = baseList.stream()
                    .filter(artist -> artist.getName().toLowerCase().contains(lowercaseQuery))
                    .collect(Collectors.toList());
        }
        
        updateArtistsList(filteredArtists);
    }

    private void loadArtists() {
        if (!isAdded() || getContext() == null) return;
        
        // Load all artists
        db.collection("artists").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded() || getContext() == null) return;
                
                allArtists.clear();
                for (var doc : queryDocumentSnapshots) {
                    Artist artist = doc.toObject(Artist.class);
                    artist.setId(doc.getId());
                    allArtists.add(artist);
                }
                
                // After loading all artists, load followed artists
                loadFollowedArtists();
            })
            .addOnFailureListener(e -> {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Lỗi khi tải danh sách nghệ sĩ", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadFollowedArtists() {
        if (!isAdded() || getContext() == null) return;
        
        // Kiểm tra currentUserId trước khi sử dụng
        if (currentUserId == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
            } else {
                // Người dùng chưa đăng nhập, chỉ hiển thị empty state
                showEmptyState(true);
                return;
            }
        }
        
        db.collection("users").document(currentUserId)
            .collection("followedArtists").get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded() || getContext() == null) return;
                
                followedArtists.clear();
                for (var doc : queryDocumentSnapshots) {
                    String artistId = doc.getId();
                    // Find artist in allArtists list
                    for (Artist artist : allArtists) {
                        if (artist.getId().equals(artistId)) {
                            followedArtists.add(artist);
                            break;
                        }
                    }
                }
                
                // Update adapter with followed artists
                if (artistAdapter != null) {
                    List<String> followedIds = followedArtists.stream()
                            .map(Artist::getId)
                            .collect(Collectors.toList());
                    artistAdapter.updateFollowedArtists(followedIds);
                    
                    // Update UI based on current chip selection and search query
                    filterArtists(etSearchArtist != null ? etSearchArtist.getText().toString() : "");
                }
            })
            .addOnFailureListener(e -> {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Lỗi khi tải danh sách nghệ sĩ đang theo dõi", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateArtistsList(List<Artist> artists) {
        if (artistAdapter != null) {
            artistAdapter.updateData(artists);
        }
        showEmptyState(artists.isEmpty());
    }

    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFollowedArtists.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onArtistClick(Artist artist) {
        if (!isAdded() || getContext() == null) return;

        ArtistDetailFragment artistDetailFragment = ArtistDetailFragment.newInstance(artist);

        // Add callback để xử lý khi fragment bị remove
        artistDetailFragment.setOnFragmentDismissListener(() -> {
            // Hiển thị lại RecyclerView và ẩn container khi fragment bị đóng
            rvFollowedArtists.setVisibility(View.VISIBLE);
            View container = requireView().findViewById(R.id.artistDetailContainer);
            container.setVisibility(View.GONE);
        });

        // Ẩn RecyclerView và hiển thị container
        rvFollowedArtists.setVisibility(View.GONE);
        View container = requireView().findViewById(R.id.artistDetailContainer);
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
                .replace(R.id.artistDetailContainer, artistDetailFragment)
                .addToBackStack(null)
                .commit();
        }
    }

    @Override
    public void onFollowClick(Artist artist, boolean isFollowing) {
        if (!isAdded() || getContext() == null) return;
        
        // Kiểm tra currentUserId trước khi sử dụng
        if (currentUserId == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
            } else {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để theo dõi nghệ sĩ", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        DocumentReference userDoc = db.collection("users").document(currentUserId);
        DocumentReference artistDoc = db.collection("artists").document(artist.getId());

        if (isFollowing) {
            // Follow artist
            userDoc.collection("followedArtists").document(artist.getId())
                    .set(artist)
                    .addOnSuccessListener(aVoid -> {
                        if (!isAdded() || getContext() == null) return;
                        
                        artistDoc.update("followerCount", FieldValue.increment(1));
                        followedArtists.add(artist);
                        Toast.makeText(getContext(), "Đã theo dõi " + artist.getName(), Toast.LENGTH_SHORT).show();
                        
                        // Update adapter with new followed artists list
                        if (artistAdapter != null) {
                            List<String> followedIds = followedArtists.stream()
                                    .map(Artist::getId)
                                    .collect(Collectors.toList());
                            artistAdapter.updateFollowedArtists(followedIds);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Lỗi khi theo dõi nghệ sĩ", Toast.LENGTH_SHORT).show();
                        // Revert button state
                        if (artistAdapter != null) {
                            artistAdapter.updateFollowedArtists(followedArtists.stream()
                                    .map(Artist::getId)
                                    .collect(Collectors.toList()));
                        }
                    });
        } else {
            // Unfollow artist
            userDoc.collection("followedArtists").document(artist.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        if (!isAdded() || getContext() == null) return;
                        
                        artistDoc.update("followerCount", FieldValue.increment(-1));
                        followedArtists.removeIf(a -> a.getId().equals(artist.getId()));
                        Toast.makeText(getContext(), "Đã hủy theo dõi " + artist.getName(), Toast.LENGTH_SHORT).show();
                        
                        // Update adapter with new followed artists list
                        if (artistAdapter != null) {
                            List<String> followedIds = followedArtists.stream()
                                    .map(Artist::getId)
                                    .collect(Collectors.toList());
                            artistAdapter.updateFollowedArtists(followedIds);
                        }
                        
                        // Refresh the list if we're in following tab
                        if (chipFollowing.isChecked()) {
                            filterArtists(etSearchArtist != null ? etSearchArtist.getText().toString() : "");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Lỗi khi hủy theo dõi nghệ sĩ", Toast.LENGTH_SHORT).show();
                        // Revert button state
                        if (artistAdapter != null) {
                            artistAdapter.updateFollowedArtists(followedArtists.stream()
                                    .map(Artist::getId)
                                    .collect(Collectors.toList()));
                        }
                    });
        }
    }

    public void sortArtistsByName() {
        List<Artist> artistsToSort = chipAll.isChecked() ? allArtists : followedArtists;
        
        // Sắp xếp danh sách theo tên
        artistsToSort.sort((a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()));
        
        // Cập nhật danh sách đã lọc
        filteredArtists = new ArrayList<>(artistsToSort);
        
        // Cập nhật UI
        updateArtistsList(filteredArtists);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cleanup views và adapter để tránh crash khi theme thay đổi
        if (rvFollowedArtists != null) {
            rvFollowedArtists.setAdapter(null);
        }
        artistAdapter = null;
        rvFollowedArtists = null;
        layoutEmptyState = null;
        chipGroupFilter = null;
        chipAll = null;
        chipFollowing = null;
        etSearchArtist = null;
        btnClearSearch = null;
        rootLayout = null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup data để tránh memory leak
        if (allArtists != null) {
            allArtists.clear();
        }
        if (followedArtists != null) {
            followedArtists.clear();
        }
        if (filteredArtists != null) {
            filteredArtists.clear();
        }
        db = null;
        currentUserId = null;
    }
} 