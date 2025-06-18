package com.ck.music_app.utils;

import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ck.music_app.Adapter.SelectPlaylistAdapter;
import com.ck.music_app.Dialog.LoadingDialog;
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PlaylistDialogUtils {

    public static void showAddToPlaylistDialog(Context context, Song currentSong) {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(context);
        loadingDialog.show();

        // Get user's playlists
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                List<String> playlistIds = (List<String>) documentSnapshot.get("playlistId");
                if (playlistIds == null || playlistIds.isEmpty()) {
                    loadingDialog.dismiss();
                    showCreatePlaylistDialog(context, currentSong);
                    return;
                }

                // Load all playlists
                List<Playlist> playlists = new ArrayList<>();
                AtomicInteger loadedCount = new AtomicInteger(0);

                for (String playlistId : playlistIds) {
                    FirebaseFirestore.getInstance()
                        .collection("playlists")
                        .document(playlistId)
                        .get()
                        .addOnSuccessListener(playlistDoc -> {
                            Playlist playlist = playlistDoc.toObject(Playlist.class);
                            if (playlist != null) {
                                playlist.setId(playlistId);
                                playlists.add(playlist);
                            }

                            if (loadedCount.incrementAndGet() == playlistIds.size()) {
                                loadingDialog.dismiss();
                                showPlaylistSelectionDialog(context, playlists, currentSong);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("PlaylistDialogUtils", "Error loading playlist: " + e.getMessage());
                        });
                }
            })
            .addOnFailureListener(e -> {
                loadingDialog.dismiss();
                Toast.makeText(context, "Lỗi khi tải danh sách playlist", Toast.LENGTH_SHORT).show();
            });
    }

    private static void showPlaylistSelectionDialog(Context context, List<Playlist> playlists, Song currentSong) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_playlist, null);
        
        RecyclerView rvPlaylists = dialogView.findViewById(R.id.rvPlaylists);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnCreatePlaylist = dialogView.findViewById(R.id.btnCreatePlaylist);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(context));
        SelectPlaylistAdapter adapter = new SelectPlaylistAdapter(context, playlists,
            playlist -> {
                // Add song to selected playlist
                addSongToPlaylist(context, playlist, currentSong);
            });
        rvPlaylists.setAdapter(adapter);

        AlertDialog dialog = builder.setView(dialogView).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreatePlaylist.setOnClickListener(v -> {
            dialog.dismiss();
            showCreatePlaylistDialog(context, currentSong);
        });

        dialog.show();
    }

    private static void showCreatePlaylistDialog(Context context, Song currentSong) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Tạo playlist mới");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Tạo", (dialog, which) -> {
            String playlistName = input.getText().toString();
            if (!playlistName.isEmpty()) {
                createNewPlaylist(context, playlistName, currentSong);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private static void createNewPlaylist(Context context, String playlistName, Song currentSong) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        LoadingDialog loadingDialog = new LoadingDialog(context);
        loadingDialog.show();

        String newPlaylistId = UUID.randomUUID().toString();
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(new Date());

        // Create new playlist with current song
        Playlist newPlaylist = new Playlist(
            newPlaylistId,
            playlistName,
            new ArrayList<>(Collections.singletonList(currentSong.getSongId())),
            currentSong.getCoverUrl(),
            createdAt
        );

        // Save to Firestore
        FirebaseFirestore.getInstance()
            .collection("playlists")
            .document(newPlaylistId)
            .set(newPlaylist)
            .addOnSuccessListener(aVoid -> {
                // Update user's playlist array
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .update("playlistId", FieldValue.arrayUnion(newPlaylistId))
                    .addOnSuccessListener(aVoid2 -> {
                        loadingDialog.dismiss();
                        Toast.makeText(context, 
                            "Đã tạo playlist và thêm bài hát thành công", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Broadcast playlist update
                        Intent intent = new Intent("PLAYLIST_UPDATED");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        Toast.makeText(context, 
                            "Lỗi khi cập nhật user playlist", 
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                loadingDialog.dismiss();
                Toast.makeText(context, 
                    "Lỗi khi tạo playlist", 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private static void addSongToPlaylist(Context context, Playlist playlist, Song currentSong) {
        if (currentSong == null) return;

        LoadingDialog loadingDialog = new LoadingDialog(context);
        loadingDialog.show();

        // Add song ID to playlist's songIds
        List<String> currentSongIds = playlist.getSongIds();
        if (currentSongIds.contains(currentSong.getSongId())) {
            loadingDialog.dismiss();
            Toast.makeText(context, "Bài hát đã có trong playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSongIds.add(currentSong.getSongId());

        // Update Firestore
        FirebaseFirestore.getInstance()
            .collection("playlists")
            .document(playlist.getId())
            .update("songIds", currentSongIds)
            .addOnSuccessListener(aVoid -> {
                loadingDialog.dismiss();
                Toast.makeText(context, 
                    "Đã thêm bài hát vào playlist " + playlist.getName(), 
                    Toast.LENGTH_SHORT).show();

                // Broadcast playlist update
                Intent intent = new Intent("PLAYLIST_UPDATED");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            })
            .addOnFailureListener(e -> {
                loadingDialog.dismiss();
                Toast.makeText(context, 
                    "Lỗi khi thêm bài hát vào playlist", 
                    Toast.LENGTH_SHORT).show();
            });
    }
} 