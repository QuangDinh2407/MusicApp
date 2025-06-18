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

    public void searchSongs(String query, FirestoreCallback<List<Song>> callback) {
        Log.d(TAG, "Bắt đầu tìm kiếm bài hát với từ khóa: " + query);

        db.collection(SONGS_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Song> songs = new ArrayList<>();
                        List<String> queryTokens = com.ck.music_app.utils.SearchUtils.tokenize(query);

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Song song = document.toObject(Song.class);
                            song.setSongId(document.getId());

                            boolean matches = false;

                            // Tìm kiếm trong tên bài hát
                            if (song.getTitle() != null &&
                                    (com.ck.music_app.utils.SearchUtils.isFuzzyMatch(query, song.getTitle(), 60.0) ||
                                            com.ck.music_app.utils.SearchUtils.containsAllTokens(song.getTitle(),
                                                    queryTokens))) {
                                matches = true;
                            }

                            // Tìm kiếm trong tên nghệ sĩ (nếu có thêm thông tin nghệ sĩ trong song)
                            if (!matches && song.getArtistId() != null &&
                                    (com.ck.music_app.utils.SearchUtils.isFuzzyMatch(query, song.getArtistId(), 60.0) ||
                                            com.ck.music_app.utils.SearchUtils.containsAllTokens(song.getArtistId(),
                                                    queryTokens))) {
                                matches = true;
                            }

                            if (matches) {
                                songs.add(song);
                            }
                        }

                        // Sắp xếp theo độ tương đồng với title
                        songs.sort((s1, s2) -> {
                            double score1 = com.ck.music_app.utils.SearchUtils.calculateSimilarityScore(query,
                                    s1.getTitle());
                            double score2 = com.ck.music_app.utils.SearchUtils.calculateSimilarityScore(query,
                                    s2.getTitle());
                            return Double.compare(score2, score1);
                        });

                        callback.onSuccess(songs);
                    } else {
                        Log.e(TAG, "Lỗi khi tìm kiếm bài hát", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    public void searchAll(String query, FirestoreCallback<com.ck.music_app.Model.SearchResult> callback) {
        Log.d(TAG, "Bắt đầu tìm kiếm tổng hợp với từ khóa: " + query);

        com.ck.music_app.Model.SearchResult searchResult = new com.ck.music_app.Model.SearchResult();
        int[] completedCount = { 0 };
        final int totalSearches = 4; // songs, artists, albums, playlists

        // Tìm kiếm bài hát
        searchSongs(query, new FirestoreCallback<List<com.ck.music_app.Model.Song>>() {
            @Override
            public void onSuccess(List<com.ck.music_app.Model.Song> songs) {
                searchResult.setSongs(songs);
                if (!songs.isEmpty()) {
                    searchResult.setTopSong(songs.get(0)); // Bài hát đầu tiên là top result
                }

                completedCount[0]++;
                if (completedCount[0] == totalSearches) {
                    callback.onSuccess(searchResult);
                }
            }

            @Override
            public void onError(Exception e) {
                searchResult.setSongs(new ArrayList<>());
                completedCount[0]++;
                if (completedCount[0] == totalSearches) {
                    callback.onSuccess(searchResult);
                }
            }
        });

        // Tìm kiếm nghệ sĩ
        searchArtists(query, new FirestoreCallback<List<com.ck.music_app.Model.Artist>>() {
            @Override
            public void onSuccess(List<com.ck.music_app.Model.Artist> artists) {
                searchResult.setArtists(artists);
                if (!artists.isEmpty()) {
                    searchResult.setTopArtist(artists.get(0));
                }

                completedCount[0]++;
                if (completedCount[0] == totalSearches) {
                    callback.onSuccess(searchResult);
                }
            }

            @Override
            public void onError(Exception e) {
                searchResult.setArtists(new ArrayList<>());
                completedCount[0]++;
                if (completedCount[0] == totalSearches) {
                    callback.onSuccess(searchResult);
                }
            }
        });

        // Tìm kiếm album
        searchAlbums(query, new FirestoreCallback<List<com.ck.music_app.Model.Album>>() {
            @Override
            public void onSuccess(List<com.ck.music_app.Model.Album> albums) {
                searchResult.setAlbums(albums);

                completedCount[0]++;
                if (completedCount[0] == totalSearches) {
                    callback.onSuccess(searchResult);
                }
            }

            @Override
            public void onError(Exception e) {
                searchResult.setAlbums(new ArrayList<>());
                completedCount[0]++;
                if (completedCount[0] == totalSearches) {
                    callback.onSuccess(searchResult);
                }
            }
        });

        // Tìm kiếm playlist
        searchPlaylists(query, new FirestoreCallback<List<com.ck.music_app.Model.Playlist>>() {
            @Override
            public void onSuccess(List<com.ck.music_app.Model.Playlist> playlists) {
                searchResult.setPlaylists(playlists);

                completedCount[0]++;
                if (completedCount[0] == totalSearches) {
                    callback.onSuccess(searchResult);
                }
            }

            @Override
            public void onError(Exception e) {
                searchResult.setPlaylists(new ArrayList<>());
                completedCount[0]++;
                if (completedCount[0] == totalSearches) {
                    callback.onSuccess(searchResult);
                }
            }
        });
    }

    public void searchArtists(String query, FirestoreCallback<List<com.ck.music_app.Model.Artist>> callback) {
        Log.d(TAG, "Bắt đầu tìm kiếm nghệ sĩ với từ khóa: " + query);

        db.collection("artists")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<com.ck.music_app.Model.Artist> artists = new ArrayList<>();
                        List<String> queryTokens = com.ck.music_app.utils.SearchUtils.tokenize(query);

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            com.ck.music_app.Model.Artist artist = document
                                    .toObject(com.ck.music_app.Model.Artist.class);
                            artist.setId(document.getId());

                            // Fuzzy search cho tên nghệ sĩ
                            if (artist.getName() != null &&
                                    (com.ck.music_app.utils.SearchUtils.isFuzzyMatch(query, artist.getName(), 60.0) ||
                                            com.ck.music_app.utils.SearchUtils.containsAllTokens(artist.getName(),
                                                    queryTokens))) {
                                artists.add(artist);
                            }
                        }

                        // Sắp xếp theo độ tương đồng
                        artists.sort((a1, a2) -> {
                            double score1 = com.ck.music_app.utils.SearchUtils.calculateSimilarityScore(query,
                                    a1.getName());
                            double score2 = com.ck.music_app.utils.SearchUtils.calculateSimilarityScore(query,
                                    a2.getName());
                            return Double.compare(score2, score1);
                        });

                        callback.onSuccess(artists);
                    } else {
                        Log.e(TAG, "Lỗi khi tìm kiếm nghệ sĩ", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    public void searchAlbums(String query, FirestoreCallback<List<com.ck.music_app.Model.Album>> callback) {
        Log.d(TAG, "Bắt đầu tìm kiếm album với từ khóa: " + query);

        db.collection(ALBUMS_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<com.ck.music_app.Model.Album> albums = new ArrayList<>();
                        List<String> queryTokens = com.ck.music_app.utils.SearchUtils.tokenize(query);

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            com.ck.music_app.Model.Album album = document.toObject(com.ck.music_app.Model.Album.class);
                            album.setId(document.getId());

                            // Fuzzy search cho tên album
                            if (album.getTitle() != null &&
                                    (com.ck.music_app.utils.SearchUtils.isFuzzyMatch(query, album.getTitle(), 60.0) ||
                                            com.ck.music_app.utils.SearchUtils.containsAllTokens(album.getTitle(),
                                                    queryTokens))) {
                                albums.add(album);
                            }
                        }

                        // Sắp xếp theo độ tương đồng
                        albums.sort((a1, a2) -> {
                            double score1 = com.ck.music_app.utils.SearchUtils.calculateSimilarityScore(query,
                                    a1.getTitle());
                            double score2 = com.ck.music_app.utils.SearchUtils.calculateSimilarityScore(query,
                                    a2.getTitle());
                            return Double.compare(score2, score1);
                        });

                        callback.onSuccess(albums);
                    } else {
                        Log.e(TAG, "Lỗi khi tìm kiếm album", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    public void searchPlaylists(String query, FirestoreCallback<List<com.ck.music_app.Model.Playlist>> callback) {
        Log.d(TAG, "Bắt đầu tìm kiếm playlist với từ khóa: " + query);

        db.collection(PLAYLISTS_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<com.ck.music_app.Model.Playlist> playlists = new ArrayList<>();
                        List<String> queryTokens = com.ck.music_app.utils.SearchUtils.tokenize(query);

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            com.ck.music_app.Model.Playlist playlist = document
                                    .toObject(com.ck.music_app.Model.Playlist.class);
                            playlist.setId(document.getId());

                            // Fuzzy search cho tên playlist
                            if (playlist.getName() != null &&
                                    (com.ck.music_app.utils.SearchUtils.isFuzzyMatch(query, playlist.getName(), 60.0) ||
                                            com.ck.music_app.utils.SearchUtils.containsAllTokens(playlist.getName(),
                                                    queryTokens))) {
                                playlists.add(playlist);
                            }
                        }

                        // Sắp xếp theo độ tương đồng
                        playlists.sort((p1, p2) -> {
                            double score1 = com.ck.music_app.utils.SearchUtils.calculateSimilarityScore(query,
                                    p1.getName());
                            double score2 = com.ck.music_app.utils.SearchUtils.calculateSimilarityScore(query,
                                    p2.getName());
                            return Double.compare(score2, score1);
                        });

                        callback.onSuccess(playlists);
                    } else {
                        Log.e(TAG, "Lỗi khi tìm kiếm playlist", task.getException());
                        callback.onError(task.getException());
                    }
                });
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
