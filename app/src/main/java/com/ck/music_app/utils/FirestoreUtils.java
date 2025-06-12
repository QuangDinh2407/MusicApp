package com.ck.music_app.utils;

import com.ck.music_app.Model.Song;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class FirestoreUtils {
    private static final String SONGS_COLLECTION = "songs";
    private static final String ALBUMS_COLLECTION = "albums";
    private static final String PLAYLISTS_COLLECTION = "playlists";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static void getAllSongs(FirestoreCallback<List<Song>> callback) {
        Log.d("FirestoreUtils", "Bắt đầu lấy danh sách bài hát");
        db.collection(SONGS_COLLECTION).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    List<Song> songs = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        //Song song = new Song();
                        Song song = document.toObject(Song.class);
                        song.setSongId(document.getId());
//                        List<String> albumIds = document.get("albumId") instanceof List
//                                ? (List<String>) document.get("albumId")
//                                : new ArrayList<>();
//                        song.setAlbumId(albumIds);
//                        song.setArtistId(document.getString("artistId"));
//                        song.setAudioUrl(document.getString("audioUrl"));
//                        song.setCoverUrl(document.getString("coverUrl"));
//                        song.setCreateAt(document.getString("createdAt"));
//                        song.setDuration(document.getLong("duration") != null ? document.getLong("duration").intValue() : 0);
//                        song.setGenreId(document.getString("genreId"));
//                        song.setLikeCount(document.getLong("likeCount") != null ? document.getLong("likeCount").intValue() : 0);
//                        song.setTitle(document.getString("title"));
//                        song.setViewCount(document.getLong("viewCount") != null ? document.getLong("viewCount").intValue() : 0);
                        songs.add(song);

                    }
                    callback.onSuccess(songs);
                } else {
                    Log.e("FirestoreUtils", "Lỗi khi lấy danh sách bài hát", task.getException());
                    callback.onError(task.getException());
                }
            });
    }

    public static void getSongsByAlbumId(String albumId, FirestoreCallback<List<Song>> callback) {
        Log.d("FirestoreUtils", "Bắt đầu lấy bài hát từ album: " + albumId);
        db.collection(SONGS_COLLECTION)
            .whereArrayContains("albumId", albumId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("FirestoreUtils", "Lấy bài hát từ album thành công");
                    List<Song> songs = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Song song = document.toObject(Song.class);
                        song.setSongId(document.getId());
                        songs.add(song);
                        Log.d("FirestoreUtils", "Bài hát trong album: " + song.getTitle() 
                            + "\n- ID: " + song.getSongId()
                            + "\n- Album: " + song.getAlbumId()
                            + "\n- Artist: " + song.getArtistId()
                            + "\n- Duration: " + song.getDuration()
                            + "\n- Views: " + song.getViewCount()
                            + "\n- Likes: " + song.getLikeCount()
                            + "\n-------------------");
                    }
                    callback.onSuccess(songs);
                } else {
                    Log.e("FirestoreUtils", "Lỗi khi lấy bài hát từ album", task.getException());
                    callback.onError(task.getException());
                }
            });
    }

    public static void getSongsByPlaylistId(String playlistId, FirestoreCallback<List<Song>> callback) {
        Log.d("FirestoreUtils", "Bắt đầu lấy bài hát từ playlist: " + playlistId);
        db.collection(PLAYLISTS_COLLECTION).document(playlistId).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<String> songIds = (List<String>) task.getResult().get("songIds");
                    if (songIds != null && !songIds.isEmpty()) {
                        Log.d("FirestoreUtils", "Tìm thấy " + songIds.size() + " bài hát trong playlist");
                        getSongsByIds(songIds, callback);
                    } else {
                        Log.d("FirestoreUtils", "Không tìm thấy bài hát nào trong playlist");
                        callback.onSuccess(new ArrayList<>());
                    }
                } else {
                    Log.e("FirestoreUtils", "Lỗi khi lấy bài hát từ playlist", task.getException());
                    callback.onError(task.getException());
                }
            });
    }

    private static void getSongsByIds(List<String> songIds, FirestoreCallback<List<Song>> callback) {
        List<Song> songs = new ArrayList<>();
        int[] completedCount = {0};

        for (String songId : songIds) {
            db.collection(SONGS_COLLECTION).document(songId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        Song song = document.toObject(Song.class);
                        song.setSongId(document.getId());
                        songs.add(song);
                        Log.d("FirestoreUtils", "Bài hát trong playlist: " + song.getTitle() 
                            + "\n- ID: " + song.getSongId()
                            + "\n- Album: " + song.getAlbumId()
                            + "\n- Artist: " + song.getArtistId()
                            + "\n- Duration: " + song.getDuration()
                            + "\n- Views: " + song.getViewCount()
                            + "\n- Likes: " + song.getLikeCount()
                            + "\n-------------------");
                    }

                    completedCount[0]++;
                    if (completedCount[0] == songIds.size()) {
                        callback.onSuccess(songs);
                    }
                });
        }
    }

    // Tìm kiếm bài hát theo tên
//    public static List<Song> searchSongs(String query) {
//        try {
//            QuerySnapshot snapshot = db.collection(SONGS_COLLECTION)
//                    .whereGreaterThanOrEqualTo("title", query)
//                    .whereLessThanOrEqualTo("title", query + '\uf8ff')
//                    .get();
//
//            List<Song> songs = new ArrayList<>();
//            for (var doc : snapshot.getDocuments()) {
//                Song song = doc.toObject(Song.class);
//                if (song != null) {
//                    songs.add(song);
//                }
//            }
//            return songs;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new ArrayList<>();
//        }
//    }

    // Thêm bài hát mới
//    public static void addSong(Song song) {
//        db.collection(SONGS_COLLECTION)
//                .document(song.getSongId())
//                .set(song);
//    }
//
//    // Cập nhật bài hát
//    public static void updateSong(Song song) {
//        db.collection(SONGS_COLLECTION)
//                .document(song.getSongId())
//                .set(song);
//    }
//
//    // Xóa bài hát
//    public static void deleteSong(String songId) {
//        db.collection(SONGS_COLLECTION)
//                .document(songId)
//                .delete();
//    }
}
