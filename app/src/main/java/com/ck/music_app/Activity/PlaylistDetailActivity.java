package com.ck.music_app.Activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Adapter.PlaylistSongAdapter;
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity implements PlaylistSongAdapter.OnSongActionListener {
    private Playlist playlist;
    private FirebaseFirestore db;
    private List<Song> songs = new ArrayList<>();
    private PlaylistSongAdapter adapter;
    
    private ImageView imgPlaylistCover;
    private TextView tvPlaylistName;
    private TextView tvSongCount;
    private EditText etSearch;
    private RecyclerView rvSongs;
    private ExtendedFloatingActionButton fabAddSongs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        // Get playlist from intent
        playlist = (Playlist) getIntent().getSerializableExtra("playlist");
        if (playlist == null) {
            Toast.makeText(this, "Không thể tải thông tin playlist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        loadPlaylistSongs();
        setupSearch();
        setupAddSongsButton();
    }

    private void initializeViews() {
        imgPlaylistCover = findViewById(R.id.imgPlaylistCover);
        tvPlaylistName = findViewById(R.id.tvPlaylistName);
        tvSongCount = findViewById(R.id.tvSongCount);
        etSearch = findViewById(R.id.etSearch);
        rvSongs = findViewById(R.id.rvSongs);
        fabAddSongs = findViewById(R.id.fabAddSongs);

        // Set playlist info
        tvPlaylistName.setText(playlist.getName());
        Glide.with(this)
            .load(playlist.getCoverUrl())
            .placeholder(R.drawable.love)
            .error(R.drawable.love)
            .into(imgPlaylistCover);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
    }

    private void setupRecyclerView() {
        adapter = new PlaylistSongAdapter(this, songs, this);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        rvSongs.setAdapter(adapter);
    }

    private void loadPlaylistSongs() {
        if (playlist.getSongIds() == null || playlist.getSongIds().isEmpty()) {
            updateSongCount(0);
            return;
        }

        for (String songId : playlist.getSongIds()) {
            db.collection("songs").document(songId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Song song = documentSnapshot.toObject(Song.class);
                    if (song != null) {
                        songs.add(song);
                        adapter.notifyItemInserted(songs.size() - 1);
                        updateSongCount(songs.size());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải bài hát: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void updateSongCount(int count) {
        tvSongCount.setText(count + " bài hát");
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                if (query.isEmpty()) {
                    adapter.updateSongs(songs);
                } else {
                    List<Song> filteredSongs = new ArrayList<>();
                    for (Song song : songs) {
                        if (song.getTitle().toLowerCase().contains(query)) {
                            filteredSongs.add(song);
                        }
                    }
                    adapter.updateSongs(filteredSongs);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupAddSongsButton() {
        fabAddSongs.setOnClickListener(v -> showAddSongsDialog());
    }

    private void showAddSongsDialog() {
        // Query all songs not in playlist
        Query query = db.collection("songs");
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Song> allSongs = new ArrayList<>();
            List<Song> songsToAdd = new ArrayList<>();
            
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                Song song = document.toObject(Song.class);
                if (song != null && !playlist.getSongIds().contains(song.getSongId())) {
                    allSongs.add(song);
                }
            }

            // Create multi-choice dialog
            String[] songTitles = new String[allSongs.size()];
            boolean[] checkedItems = new boolean[allSongs.size()];
            
            for (int i = 0; i < allSongs.size(); i++) {
                songTitles[i] = allSongs.get(i).getTitle();
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thêm bài hát")
                .setMultiChoiceItems(songTitles, checkedItems, (dialog1, which, isChecked) -> {
                    if (isChecked) {
                        songsToAdd.add(allSongs.get(which));
                    } else {
                        songsToAdd.remove(allSongs.get(which));
                    }
                })
                .setPositiveButton("Thêm", (dialog1, which) -> {
                    if (!songsToAdd.isEmpty()) {
                        addSongsToPlaylist(songsToAdd);
                    }
                })
                .setNegativeButton("Hủy", null)
                .create();

            dialog.show();
        });
    }

    private void addSongsToPlaylist(List<Song> songsToAdd) {
        List<String> newSongIds = new ArrayList<>();
        for (Song song : songsToAdd) {
            newSongIds.add(song.getSongId());
        }

        // Update playlist document
        db.collection("playlists").document(playlist.getId())
            .update("songIds", newSongIds)
            .addOnSuccessListener(aVoid -> {
                playlist.setSongIds(newSongIds);
                songs.addAll(songsToAdd);
                adapter.notifyDataSetChanged();
                updateSongCount(songs.size());
                Toast.makeText(this, "Đã thêm " + songsToAdd.size() + " bài hát", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi khi thêm bài hát: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onSongClick(Song song) {
        // TODO: Play song
        Toast.makeText(this, "Đang phát: " + song.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemoveClick(Song song, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Xóa bài hát")
            .setMessage("Bạn có chắc chắn muốn xóa bài hát này khỏi playlist?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                List<String> updatedSongIds = new ArrayList<>(playlist.getSongIds());
                updatedSongIds.remove(song.getSongId());

                db.collection("playlists").document(playlist.getId())
                    .update("songIds", updatedSongIds)
                    .addOnSuccessListener(aVoid -> {
                        playlist.setSongIds(updatedSongIds);
                        adapter.removeSong(position);
                        updateSongCount(songs.size());
                        Toast.makeText(this, "Đã xóa bài hát khỏi playlist", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi xóa bài hát: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    @Override
    public void onMoreClick(Song song, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.popup_playlist_song);
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_play) {
                // TODO: Play song
                return true;
            } else if (id == R.id.action_add_to_queue) {
                // TODO: Add to queue
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 