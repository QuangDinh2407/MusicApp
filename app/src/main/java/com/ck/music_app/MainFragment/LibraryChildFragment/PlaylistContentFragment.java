package com.ck.music_app.MainFragment.LibraryChildFragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.ck.music_app.R;
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.Adapter.PlaylistAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.app.ProgressDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import androidx.cardview.widget.CardView;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.LinearLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Button;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaylistContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaylistContentFragment extends Fragment implements PlaylistAdapter.OnPlaylistListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private RecyclerView rvPlaylist;
    private PlaylistAdapter adapter;
    private List<Playlist> playlistList;
    private FirebaseFirestore db;
    private RecyclerView rvSuggestedPlaylists;
    private LinearLayout layoutEmptyState;
    private ChipGroup chipGroupFilter;
    private Chip chipRecent, chipAlphabetical, chipCreator;

    private Playlist selectedPlaylistForContextMenu;

    public PlaylistContentFragment() {
        // Required empty public constructor
    }

    public static PlaylistContentFragment newInstance() {
        return new PlaylistContentFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            playlistList = new ArrayList<>();
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            mAuthListener = firebaseAuth -> {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_playlist_content, container, false);
            
            // Khởi tạo views
            rvPlaylist = view.findViewById(R.id.rvPlaylist);
            rvSuggestedPlaylists = view.findViewById(R.id.rvSuggestedPlaylists);
            layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
            chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
            chipRecent = view.findViewById(R.id.chipRecent);
            chipAlphabetical = view.findViewById(R.id.chipAlphabetical);

            // Khởi tạo adapter
            adapter = new PlaylistAdapter(getContext(), playlistList, this);

            // Thiết lập RecyclerView
            rvPlaylist.setLayoutManager(new LinearLayoutManager(getContext()));
            rvPlaylist.setAdapter(adapter);

            // Register RecyclerView for context menu
            registerForContextMenu(rvPlaylist);

            // Thiết lập RecyclerView cho gợi ý (hiện tại không có dữ liệu)
            rvSuggestedPlaylists.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            // rvSuggestedPlaylists.setAdapter(new SuggestedPlaylistAdapter(getContext(), new ArrayList<>())); // Cần adapter riêng cho gợi ý

            // Set default selection
            chipRecent.setChecked(true);

            return view;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return inflater.inflate(R.layout.fragment_playlist_content, container, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mAuth.getCurrentUser() != null) {
            loadUserPlaylists();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        Toast.makeText(getContext(), "Đã nhấp vào: " + playlist.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditClick(Playlist playlist) {
        showEditPlaylistDialog(playlist);
    }

    @Override
    public void onDeleteClick(Playlist playlist) {
        deletePlaylist(playlist);
    }

    @Override
    public void onPlaylistLongClick(Playlist playlist, View view, int position) {
        selectedPlaylistForContextMenu = playlist;
        getActivity().openContextMenu(view);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.rvPlaylist) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.playlist_context_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (selectedPlaylistForContextMenu == null) {
            return super.onContextItemSelected(item);
        }

        int id = item.getItemId();
        if (id == R.id.action_edit_playlist) {
            onEditClick(selectedPlaylistForContextMenu);
            return true;
        } else if (id == R.id.action_delete_playlist) {
            onDeleteClick(selectedPlaylistForContextMenu);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    public void showAddPlaylistDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
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

        // Use a default cover URL
        String defaultCoverUrl = "https://img.freepik.com/free-photo/digital-illustration-simple-blue-heart_181624-33760.jpg?semt=ais_hybrid&w=740";

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

    private void loadUserPlaylists() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            updateEmptyState();
            return;
        }
        String userId = currentUser.getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "Không tìm thấy User ID.", Toast.LENGTH_SHORT).show();
            updateEmptyState();
            return;
        }

        db.collection("users").document(userId).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    List<String> userPlaylistIds = (List<String>) task.getResult().get("playlistId");
                    if (userPlaylistIds != null && !userPlaylistIds.isEmpty()) {
                        playlistList.clear();
                        final int[] playlistsToLoad = {userPlaylistIds.size()};

                        for (String playlistId : userPlaylistIds) {
                            if (playlistId == null || playlistId.isEmpty()) {
                                Log.e("PlaylistContentFragment", "Found null or empty playlistId in user's playlistId list.");
                                playlistsToLoad[0]--;
                                if (playlistsToLoad[0] == 0) {
                                    adapter.setPlaylists(playlistList);
                                    updateEmptyState();
                                }
                                continue;
                            }
                            db.collection("playlists").document(playlistId).get()
                                .addOnCompleteListener(playlistTask -> {
                                    playlistsToLoad[0]--;
                                    if (playlistTask.isSuccessful() && playlistTask.getResult() != null && playlistTask.getResult().exists()) {
                                        try {
                                            Playlist playlist = playlistTask.getResult().toObject(Playlist.class);
                                            if (playlist != null) {
                                                playlistList.add(playlist);
                                            } else {
                                                Log.e("PlaylistContentFragment", "Failed to convert document to Playlist object: " + playlistId);
                                            }
                                        } catch (Exception e) {
                                            Log.e("PlaylistContentFragment", "Error deserializing playlist " + playlistId + ": " + e.getMessage(), e);
                                            Toast.makeText(getContext(), "Lỗi tải playlist: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        String errorMsg = playlistTask.getException() != null ? playlistTask.getException().getMessage() : "Unknown error";
                                        Log.e("PlaylistContentFragment", "Error loading playlist " + playlistId + ": " + errorMsg);
                                        Toast.makeText(getContext(), "Lỗi tải playlist: " + errorMsg, Toast.LENGTH_SHORT).show();
                                    }

                                    if (playlistsToLoad[0] == 0) {
                                        adapter.setPlaylists(playlistList);
                                        updateEmptyState();
                                    }
                                });
                        }
                    } else {
                        playlistList.clear();
                        adapter.setPlaylists(playlistList);
                        updateEmptyState();
                        Toast.makeText(getContext(), "Bạn chưa có playlist nào.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Log.e("PlaylistContentFragment", "Error loading user playlists: " + errorMsg);
                    Toast.makeText(getContext(), "Lỗi khi tải danh sách playlist của người dùng: " + errorMsg, Toast.LENGTH_SHORT).show();
                    playlistList.clear();
                    adapter.setPlaylists(playlistList);
                    updateEmptyState();
                }
            });
    }

    private void updateEmptyState() {
        if (playlistList.isEmpty()) {
            rvPlaylist.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvPlaylist.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
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
                // TODO: Sort by recent
            } else if (checkedId == R.id.chipAlphabetical) {
                // TODO: Sort alphabetically
            }
        });
    }
} 