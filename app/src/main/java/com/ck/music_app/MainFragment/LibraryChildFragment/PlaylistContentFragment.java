package com.ck.music_app.MainFragment.LibraryChildFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.PopupMenu;

import com.ck.music_app.Activity.PlaylistDetailActivity;
import com.ck.music_app.Adapter.PlaylistAdapter;
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import android.app.ProgressDialog;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaylistContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaylistContentFragment extends Fragment implements PlaylistAdapter.OnPlaylistListener {

    private static final String PREF_NAME = "PlaylistPrefs";
    private static final String KEY_SELECTED_PLAYLIST = "selectedPlaylistId";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private SharedPreferences prefs;

    private RecyclerView rvPlaylists;
    private PlaylistAdapter adapter;
    private List<Playlist> playlistList;
    private FirebaseFirestore db;
    private LinearLayout layoutEmptyState;
    private ChipGroup chipGroupFilter;
    private Chip chipRecent, chipAlphabetical;

    private BroadcastReceiver playlistUpdateReceiver;

    private boolean isLoading = false;
    private ProgressDialog progressDialog;

    public PlaylistContentFragment() {
        // Required empty public constructor
    }

    public static PlaylistContentFragment newInstance() {
        return new PlaylistContentFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        // Sử dụng requireContext() thay vì getActivity() để tránh crash khi theme thay đổi
        // prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Khởi tạo prefs sau trong onAttach hoặc onCreateView

        // Khởi tạo BroadcastReceiver cho cập nhật playlist
        playlistUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case "PLAYLIST_UPDATED":
                    Log.d("PlaylistContentFragment", "Received playlist update broadcast");
                    refreshPlaylists();
                        break;
                    case "PLAYLIST_SONGS_CHANGED":
                        String playlistId = intent.getStringExtra("playlistId");
                        ArrayList<String> songIds = intent.getStringArrayListExtra("songIds");
                        Log.d("PlaylistContentFragment", "Received songs changed broadcast for playlist: " + playlistId + " with " + (songIds != null ? songIds.size() : 0) + " songs");
                        if (playlistId != null) {
                            onPlaylistSongsChanged(playlistId, songIds);
                        }
                        break;
                }
            }
        };

        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // User is signed in
                loadUserPlaylists();
            } else {
                // User is signed out
                playlistList.clear();
                if (adapter != null) {
                    adapter.setPlaylists(playlistList);
                }
                updateEmptyState();
                Toast.makeText(getContext(), "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_content, container, false);
        
        // Khởi tạo SharedPreferences sau khi có context đảm bảo
        if (prefs == null) {
            prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        
        // Khởi tạo views
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipRecent = view.findViewById(R.id.chipRecent);
        chipAlphabetical = view.findViewById(R.id.chipAlphabetical);

        // Khởi tạo adapter
        adapter = new PlaylistAdapter(getContext(), playlistList, this);
        rvPlaylists.setAdapter(adapter);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set default selection and sort
        chipRecent.setChecked(true);
        setupListeners();

        // Xử lý sự kiện click cho nút tạo playlist đầu tiên
        Button btnCreateFirstPlaylist = view.findViewById(R.id.btnCreateFirstPlaylist);
        btnCreateFirstPlaylist.setOnClickListener(v -> showAddPlaylistDialog());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Đảm bảo mAuth được khởi tạo
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            loadUserPlaylists();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Đảm bảo mAuth được khởi tạo trước khi add AuthStateListener
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        
        if (mAuth != null && mAuthListener != null) {
            mAuth.addAuthStateListener(mAuthListener);
        }
        
        // Đăng ký BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("PLAYLIST_UPDATED");
        filter.addAction("PLAYLIST_SONGS_CHANGED");
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(playlistUpdateReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null && mAuth != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        // Hủy đăng ký BroadcastReceiver
        try {
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(playlistUpdateReceiver);
        } catch (Exception e) {
            // Ignore if receiver was not registered
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Đảm bảo mAuth được khởi tạo
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            loadUserPlaylists();
        }
    }

    public void refreshPlaylists() {
        // Đảm bảo mAuth được khởi tạo và fragment vẫn attach
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        
        if (mAuth != null && mAuth.getCurrentUser() != null && isAdded() && getContext() != null) {
            loadUserPlaylists();
        }
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        if (!isAdded() || getContext() == null) return;
        
        Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
        intent.putExtra("playlistId", playlist.getId());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Playlist playlist) {
        if (!isAdded() || getContext() == null) return;
        showEditPlaylistDialog(playlist);
    }

    @Override
    public void onDeleteClick(Playlist playlist) {
        if (!isAdded() || getContext() == null) return;
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xóa playlist")
            .setMessage("Bạn có chắc chắn muốn xóa playlist này?")
            .setPositiveButton("Xóa", (dialog, which) -> deletePlaylist(playlist))
            .setNegativeButton("Hủy", null)
            .show();
    }

    @Override
    public void onPlaylistLongClick(Playlist playlist, View view, int position) {
        if (!isAdded() || getContext() == null) return;
        
        // Show popup menu
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenuInflater().inflate(R.menu.playlist_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_edit) {
                showEditPlaylistDialog(playlist);
            return true;
            } else if (itemId == R.id.menu_delete) {
                deletePlaylist(playlist);
            return true;
        }
            return false;
        });

        popup.show();
    }

    public void showAddPlaylistDialog() {
        if (!isAdded() || getContext() == null) return;
        
        // Đảm bảo mAuth được khởi tạo
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        
        FirebaseUser currentUser = (mAuth != null) ? mAuth.getCurrentUser() : null;
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để tạo playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inflate custom layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_playlist, null);
        EditText input = dialogView.findViewById(R.id.etPlaylistName);
        TextView tvCharCount = dialogView.findViewById(R.id.tvCharCount);

        // Update character count
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/50");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
            .setTitle("Tạo Playlist Mới")
            .setView(dialogView)
            .setPositiveButton("Tạo", null) // Set null here to prevent auto-dismiss
            .setNegativeButton("Hủy", (dialog1, which) -> dialog1.cancel())
            .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String playlistName = input.getText().toString().trim();
                if (playlistName.isEmpty()) {
                    input.setError("Tên playlist không được để trống");
                    return;
                }
                dialog.dismiss();
                addNewPlaylist(playlistName);
            });
        });

        dialog.show();
    }

    private void addNewPlaylist(String playlistName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để tạo playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate playlist name
        if (playlistName.length() < 1 || playlistName.length() > 50) {
            Toast.makeText(getContext(), "Tên playlist phải từ 1-50 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Đang tạo playlist...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String userId = currentUser.getUid();
        String newPlaylistId = UUID.randomUUID().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String createdAtString = sdf.format(new Date());

        // Use a default cover URL for empty playlist
        String defaultCoverUrl = "https://img.freepik.com/free-photo/digital-illustration-simple-blue-heart_181624-33760.jpg?semt=ais_hybrid&w=740";

        // Create new playlist with default cover
        Playlist newPlaylist = new Playlist(
            newPlaylistId, 
            playlistName, 
            new ArrayList<>(),
            defaultCoverUrl,
            createdAtString
        );

        // First check if playlist name already exists for this user
        db.collection("playlists")
            .whereEqualTo("name", playlistName)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean nameExists = false;
                    for (DocumentSnapshot doc : task.getResult()) {
                        Playlist existingPlaylist = doc.toObject(Playlist.class);
                        if (existingPlaylist != null && existingPlaylist.getName().equals(playlistName)) {
                            nameExists = true;
                            break;
                        }
                    }

                    if (nameExists) {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Tên playlist đã tồn tại", Toast.LENGTH_SHORT).show();
                    } else {
                        // Create new playlist
                        db.collection("playlists").document(newPlaylistId)
                            .set(newPlaylist)
                            .addOnSuccessListener(aVoid -> {
                                // Update user's playlist array
                                db.collection("users").document(userId)
                                    .update("playlistId", FieldValue.arrayUnion(newPlaylistId))
                                    .addOnSuccessListener(aVoid2 -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(getContext(), "Đã thêm playlist thành công", Toast.LENGTH_SHORT).show();
                                        loadUserPlaylists();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Log.e("PlaylistContent", "Error updating user playlist", e);
                                        Toast.makeText(getContext(), 
                                            "Lỗi khi cập nhật user playlist: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                        // Rollback playlist creation
                                        db.collection("playlists").document(newPlaylistId).delete();
                                    });
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Log.e("PlaylistContent", "Error creating playlist", e);
                                Toast.makeText(getContext(), 
                                    "Lỗi khi tạo playlist mới: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            });
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Lỗi khi kiểm tra tên playlist", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showEditPlaylistDialog(Playlist playlist) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để sửa playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Sửa Playlist");

        final EditText input = new EditText(getContext());
        input.setHint("Tên Playlist");
        input.setText(playlist.getName());
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newPlaylistName = input.getText().toString().trim();
            if (newPlaylistName.isEmpty()) {
                Toast.makeText(getContext(), "Tên playlist không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }
            updatePlaylistName(playlist.getId(), newPlaylistName);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updatePlaylistName(String playlistId, String newName) {
        db.collection("playlists").document(playlistId)
            .update("name", newName)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Đã cập nhật tên playlist thành công", Toast.LENGTH_SHORT).show();
                loadUserPlaylists();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Lỗi khi cập nhật tên playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showLoading() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Đang tải danh sách playlist...");
            progressDialog.setCancelable(false);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadUserPlaylists() {
        if (isLoading) return; // Tránh load nhiều lần
        isLoading = true;

        // Đảm bảo mAuth được khởi tạo
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }

        FirebaseUser currentUser = (mAuth != null) ? mAuth.getCurrentUser() : null;
        if (currentUser == null) {
            isLoading = false;
            updateEmptyState();
            return;
        }

        String userId = currentUser.getUid();
        if (userId == null) {
            isLoading = false;
            updateEmptyState();
            return;
        }

//        showLoading();

        // Clear existing playlists
        playlistList = new ArrayList<>();

        db.collection("users").document(userId).get()
            .addOnCompleteListener(task -> {
                if (!isAdded()) {
                    isLoading = false;
                    hideLoading();
                    return;
                }

                if (task.isSuccessful() && task.getResult() != null) {
                    List<String> userPlaylistIds = (List<String>) task.getResult().get("playlistId");
                    if (userPlaylistIds != null && !userPlaylistIds.isEmpty()) {
                        // Sử dụng LinkedHashSet để loại bỏ duplicate và giữ thứ tự
                        Set<String> uniqueIds = new LinkedHashSet<>(userPlaylistIds);
                        loadPlaylistDetails(new ArrayList<>(uniqueIds));
                    } else {
                        isLoading = false;
                        hideLoading();
                        updateEmptyState();
                    }
                } else {
                    isLoading = false;
                    hideLoading();
                    updateEmptyState();
                    Log.e("PlaylistContentFragment", "Error loading user playlists", task.getException());
                }
            })
            .addOnFailureListener(e -> {
                isLoading = false;
                hideLoading();
                updateEmptyState();
                Log.e("PlaylistContentFragment", "Error loading user data", e);
            });
    }

    private void loadPlaylistDetails(List<String> playlistIds) {
        int totalPlaylists = playlistIds.size();
        AtomicInteger loadedCount = new AtomicInteger(0);

        for (String playlistId : playlistIds) {
            if (playlistId == null || playlistId.isEmpty()) {
                if (loadedCount.incrementAndGet() == totalPlaylists) {
                    finishLoading();
                }
                continue;
            }

            db.collection("playlists").document(playlistId).get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;

                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        try {
                            Playlist playlist = task.getResult().toObject(Playlist.class);
                            if (playlist != null) {
                                playlist.setId(playlistId);
                                if (playlist.getSongIds() == null) {
                                    playlist.setSongIds(new ArrayList<>());
                                }
                                playlistList.add(playlist);
                            }
                        } catch (Exception e) {
                            Log.e("PlaylistContentFragment", "Error processing playlist", e);
                        }
                    }

                    if (loadedCount.incrementAndGet() == totalPlaylists) {
                        finishLoading();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PlaylistContentFragment", "Error loading playlist: " + playlistId, e);
                    if (loadedCount.incrementAndGet() == totalPlaylists) {
                        finishLoading();
                    }
                });
        }
    }

    private void finishLoading() {
        isLoading = false;
        hideLoading();

        // Sort playlists based on current filter
        if (!playlistList.isEmpty()) {
            if (chipRecent.isChecked()) {
                sortPlaylistsByRecent();
            } else if (chipAlphabetical.isChecked()) {
                sortPlaylistsAlphabetically();
            }
        }

        // Cập nhật UI
        updatePlaylistAdapter();
    }

    private void updatePlaylistAdapter() {
        if (!isAdded() || adapter == null) return;

        adapter.setPlaylists(new ArrayList<>(playlistList));
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (!isAdded()) return;

        if (playlistList.isEmpty()) {
            rvPlaylists.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvPlaylists.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup để tránh memory leak
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        playlistUpdateReceiver = null;
        mAuthListener = null;
        prefs = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cleanup views và adapter để tránh crash khi theme thay đổi
        if (rvPlaylists != null) {
            rvPlaylists.setAdapter(null);
        }
        adapter = null;
        rvPlaylists = null;
        layoutEmptyState = null;
        chipGroupFilter = null;
        chipRecent = null;
        chipAlphabetical = null;
    }

    private void deletePlaylist(Playlist playlist) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xóa playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
            .setTitle("Xóa Playlist")
            .setMessage("Bạn có chắc chắn muốn xóa playlist \"" + playlist.getName() + "\" không?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                String userId = currentUser.getUid();

                db.collection("playlists").document(playlist.getId()).delete()
                    .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(userId)
                            .update("playlistId", FieldValue.arrayRemove(playlist.getId()))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(), "Đã xóa playlist thành công", Toast.LENGTH_SHORT).show();
                                loadUserPlaylists();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Lỗi khi cập nhật user playlist sau khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Lỗi khi xóa playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void setupListeners() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipRecent) {
                sortPlaylistsByRecent();
            } else if (checkedId == R.id.chipAlphabetical) {
                sortPlaylistsAlphabetically();
            }
        });
    }

    private void sortPlaylistsByRecent() {
        if (playlistList != null && !playlistList.isEmpty()) {
            playlistList.sort((p1, p2) -> {
                String time1 = p1.getLastAccessedAt() != null ? p1.getLastAccessedAt() : p1.getCreatedAt();
                String time2 = p2.getLastAccessedAt() != null ? p2.getLastAccessedAt() : p2.getCreatedAt();
                return time2.compareTo(time1); // Sort in descending order (most recent first)
            });
            updatePlaylistAdapter();
        }
    }

    private void sortPlaylistsAlphabetically() {
        if (playlistList != null && !playlistList.isEmpty()) {
            playlistList.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
            updatePlaylistAdapter();
        }
    }

    private void updatePlaylistCover(String playlistId, List<String> songIds) {
        if (songIds == null || songIds.isEmpty()) {
            Log.d("PlaylistContent", "No songs in playlist, keeping default cover");
            return;
        }

        // Get the first song's data to update playlist cover
        db.collection("songs").document(songIds.get(0))
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String songImageUrl = documentSnapshot.getString("imageUrl");
                    if (songImageUrl != null && !songImageUrl.isEmpty()) {
                        // Update playlist cover with first song's image
                        db.collection("playlists").document(playlistId)
                            .update("coverUrl", songImageUrl)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("PlaylistContent", "Successfully updated playlist cover to: " + songImageUrl);
                                // Update the playlist in our local list
                                for (Playlist playlist : playlistList) {
                                    if (playlist.getId().equals(playlistId)) {
                                        playlist.setCoverUrl(songImageUrl);
                                        break;
                                    }
                                }
                                // Notify adapter of the change
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("PlaylistContent", "Error updating playlist cover", e);
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e("PlaylistContent", "Error getting first song data", e);
            });
    }

    // Thêm phương thức mới để cập nhật cover khi thêm/xóa bài hát
    public void onPlaylistSongsChanged(String playlistId, List<String> newSongIds) {
        updatePlaylistCover(playlistId, newSongIds);
    }
} 