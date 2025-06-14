package com.ck.music_app.Services;

import android.util.Log;

import com.ck.music_app.Model.Song;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.*;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static final String SONGS_COLLECTION = "songs";
    private static final String ALBUMS_COLLECTION = "albums";
    private static final String PLAYLISTS_COLLECTION = "playlists";
    private static final String USERS_COLLECTION = "users";

    private static FirebaseService instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private FirebaseService() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseService getInstance() {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    // Authentication methods
    public void registerWithEmail(String email, String password, OnAuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            createUserInFirestore(user);
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        callback.onError("Đăng ký không thành công: " + task.getException().getMessage());
                    }
                });
    }

    public void loginWithEmail(String email, String password, OnAuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            createUserIfNotExists(user);
                            callback.onSuccess(user);
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        callback.onError("Đăng nhập không thành công: " + task.getException().getMessage());
                    }
                });
    }

    public void loginWithGoogle(String idToken, OnAuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            createUserIfNotExists(user);
                            callback.onSuccess(user);
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.linkWithCredential(credential)
                                        .addOnCompleteListener(linkTask -> {
                                            if (linkTask.isSuccessful()) {
                                                createUserIfNotExists(user);
                                                callback.onSuccess(user);
                                            } else {
                                                callback.onError("Liên kết Google thất bại: " + linkTask.getException().getMessage());
                                            }
                                        });
                            } else {
                                callback.onError("Vui lòng đăng nhập bằng Facebook hoặc Email trước để liên kết Google.");
                            }
                        } else {
                            callback.onError("Đăng nhập Google không thành công: " + e.getMessage());
                        }
                    }
                });
    }

    public void loginWithFacebook(String token, OnAuthCallback callback) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            createUserIfNotExists(user);
                            callback.onSuccess(user);
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.linkWithCredential(credential)
                                        .addOnCompleteListener(linkTask -> {
                                            if (linkTask.isSuccessful()) {
                                                createUserIfNotExists(user);
                                                callback.onSuccess(user);
                                            } else {
                                                callback.onError("Liên kết Facebook thất bại: " + linkTask.getException().getMessage());
                                            }
                                        });
                            } else {
                                callback.onError("Vui lòng đăng nhập bằng Google trước để liên kết Facebook.");
                            }
                        } else {
                            callback.onError("Đăng nhập Facebook không thành công: " + e.getMessage());
                        }
                    }
                });
    }

    public void logout() {
        mAuth.signOut();
    }

    // Firestore methods
    private void createUserInFirestore(FirebaseUser user) {
        if (user == null) return;
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("DateOfBirth", "");
        userMap.put("Email", user.getEmail() != null ? user.getEmail() : "");
        userMap.put("Name", user.getDisplayName() != null ? user.getDisplayName() : "");
        userMap.put("songPlaying", "");
        db.collection(USERS_COLLECTION).document(user.getUid()).set(userMap);
    }

    private void createUserIfNotExists(FirebaseUser user) {
        if (user == null) return;
        db.collection(USERS_COLLECTION)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("DateOfBirth", "");
                        userMap.put("Email", user.getEmail() != null ? user.getEmail() : "");
                        userMap.put("Name", user.getDisplayName() != null ? user.getDisplayName() : "");
                        userMap.put("songPlaying", "");
                        db.collection(USERS_COLLECTION).document(user.getUid()).set(userMap);
                    }
                });
    }

    // Song methods
    public void getAllSongs(FirestoreCallback<List<Song>> callback) {
        Log.d(TAG, "Bắt đầu lấy danh sách bài hát");
        db.collection(SONGS_COLLECTION).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Song> songs = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Song song = document.toObject(Song.class);
                            song.setSongId(document.getId());
                            songs.add(song);
                        }
                        callback.onSuccess(songs);
                    } else {
                        Log.e(TAG, "Lỗi khi lấy danh sách bài hát", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    public void getSongsByAlbumId(String albumId, FirestoreCallback<List<Song>> callback) {
        Log.d(TAG, "Bắt đầu lấy bài hát từ album: " + albumId);
        db.collection(SONGS_COLLECTION)
                .whereArrayContains("albumId", albumId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Song> songs = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Song song = document.toObject(Song.class);
                            song.setSongId(document.getId());
                            Log.d(TAG, "Loaded song: " + song.getTitle() + " with ID: " + document.getId());
                            songs.add(song);
                        }
                        callback.onSuccess(songs);
                    } else {
                        Log.e(TAG, "Lỗi khi lấy bài hát từ album", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    public void getSongsByPlaylistId(String playlistId, FirestoreCallback<List<Song>> callback) {
        Log.d(TAG, "Bắt đầu lấy bài hát từ playlist: " + playlistId);
        db.collection(PLAYLISTS_COLLECTION)
                .document(playlistId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> songIds = (List<String>) task.getResult().get("songIds");
                        if (songIds != null && !songIds.isEmpty()) {
                            getSongsByIds(songIds, callback);
                        } else {
                            callback.onSuccess(new ArrayList<>());
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi lấy bài hát từ playlist", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    private void getSongsByIds(List<String> songIds, FirestoreCallback<List<Song>> callback) {
        List<Song> songs = new ArrayList<>();
        int[] completedCount = {0};

        for (String songId : songIds) {
            db.collection(SONGS_COLLECTION)
                    .document(songId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            Song song = document.toObject(Song.class);
                            song.setSongId(document.getId());
                            songs.add(song);
                        }

                        completedCount[0]++;
                        if (completedCount[0] == songIds.size()) {
                            callback.onSuccess(songs);
                        }
                    });
        }
    }

    public void getSongById(String songId, FirestoreCallback<Song> callback) {
        if (songId == null || songId.isEmpty()) {
            callback.onError(new Exception("Song ID is empty"));
            return;
        }

        Log.d(TAG, "Getting song by ID: " + songId);
        db.collection(SONGS_COLLECTION)
                .document(songId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Song song = document.toObject(Song.class);
                            song.setSongId(document.getId());
                            Log.d(TAG, "Found song: " + song.getTitle() + " with ID: " + document.getId());
                            callback.onSuccess(song);
                        } else {
                            callback.onError(new Exception("Song not found"));
                        }
                    } else {
                        Log.e(TAG, "Error getting song by ID", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    // User methods
    public void updateSongPlaying(String songId, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        Log.d(TAG, "Updating songPlaying for user: " + currentUser.getUid());
        Log.d(TAG, "SongId to save: " + songId);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("songPlaying", songId);

        db.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully updated songPlaying: " + songId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating songPlaying", e);
                    callback.onError(e);
                });
    }

    public void getCurrentUserSongPlaying(FirestoreCallback<String> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String songPlaying = documentSnapshot.getString("songPlaying");
                        callback.onSuccess(songPlaying != null ? songPlaying : "");
                    } else {
                        callback.onSuccess("");
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    // Callback interfaces
    public interface OnAuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
} 