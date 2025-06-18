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
    private static final String ARTISTS_COLLECTION = "artists";

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

    public void loginWithFacebook(String token, String facebookName, String facebookEmail, String profilePicUrl, OnAuthCallback callback) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            createUserIfNotExists(user, facebookName, facebookEmail, profilePicUrl);
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
                                                createUserIfNotExists(user, facebookName, facebookEmail, profilePicUrl);
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
        createUserIfNotExists(user, null, null, null);
    }

    private void createUserIfNotExists(FirebaseUser user) {
        createUserIfNotExists(user, null, null, null);
    }

    private void createUserIfNotExists(FirebaseUser user, String fbName, String fbEmail, String fbProfilePicUrl) {
        if (user == null) return;
        db.collection(USERS_COLLECTION)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("DateOfBirth", "");
                        
                        // Ưu tiên email từ Facebook nếu có
                        String email = fbEmail != null ? fbEmail : (user.getEmail() != null ? user.getEmail() : "");
                        userMap.put("Email", email);
                        
                        // Ưu tiên tên từ Facebook nếu có
                        String name = fbName != null ? fbName : (user.getDisplayName() != null ? user.getDisplayName() : "");
                        userMap.put("Name", name);
                        
                        // Thêm URL ảnh đại diện từ Facebook
                        if (fbProfilePicUrl != null) {
                            userMap.put("ProfilePicture", fbProfilePicUrl);
                        }
                        
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

    // Artist methods
    public void getArtistNameById(String artistId, FirestoreCallback<String> callback) {
        Log.d(TAG, "Bắt đầu lấy thông tin nghệ sĩ: " + artistId);
        db.collection(ARTISTS_COLLECTION)
                .document(artistId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String artistName = document.getString("name");
                            if (artistName != null && !artistName.isEmpty()) {
                                callback.onSuccess(artistName);
                            } else {
                                callback.onError(new Exception("Không tìm thấy tên nghệ sĩ"));
                            }
                        } else {
                            callback.onError(new Exception("Không tìm thấy nghệ sĩ với ID: " + artistId));
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi lấy thông tin nghệ sĩ", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    // User song methods
    public void getUserCurrentSong(String userId, FirestoreCallback<Song> callback) {
        Log.d(TAG, "Bắt đầu lấy bài hát đang phát của user: " + userId);
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String songId = task.getResult().getString("songPlaying");
                        if (songId != null && !songId.isEmpty()) {
                            // Nếu có songId, lấy thông tin bài hát
                            db.collection(SONGS_COLLECTION)
                                    .document(songId)
                                    .get()
                                    .addOnCompleteListener(songTask -> {
                                        if (songTask.isSuccessful() && songTask.getResult() != null && songTask.getResult().exists()) {
                                            Song song = songTask.getResult().toObject(Song.class);
                                            song.setSongId(songTask.getResult().getId());
                                            callback.onSuccess(song);
                                        } else {
                                            callback.onError(new Exception("Không tìm thấy bài hát"));
                                        }
                                    });
                        } else {
                            callback.onError(new Exception("Không có bài hát đang phát"));
                        }
                    } else {
                        Log.e(TAG, "Lỗi khi lấy thông tin user", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    public void updateUserCurrentSong(String userId, String songId, FirestoreCallback<Void> callback) {
        Log.d(TAG, "Cập nhật bài hát đang phát của user: " + userId);
        db.collection(USERS_COLLECTION)
                .document(userId)
                .update("songPlaying", songId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        Log.e(TAG, "Lỗi khi cập nhật bài hát đang phát", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    public void updateCurrentPlayingSong(Song song) {
        // Kiểm tra xem có user đang đăng nhập không và bài hát không phải local
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && song != null && song.getSongId() != null 
            && !song.getSongId().isEmpty() 
            && !song.getAudioUrl().startsWith("content://")) {
            
            updateUserCurrentSong(
                currentUser.getUid(), 
                song.getSongId(),
                new FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Đã cập nhật songPlaying thành công");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Lỗi khi cập nhật songPlaying: " + e.getMessage());
                    }
                }
            );
        }
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