package com.ck.music_app.utils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.List;

public class FavoriteAlbumUtils {
    public static final String BROADCAST_FAVORITE_UPDATED = "favorite_album_updated";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface FavoriteCallback {
        void onSuccess(boolean isFavorite);
        void onError();
    }

    public static void toggleFavoriteAlbum(Context context, String albumName, String coverUrl, boolean currentFavoriteState, FavoriteCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newFavoriteState = !currentFavoriteState;
        String userId = currentUser.getUid();

        if (newFavoriteState) {
            addToFavorites(context, userId, albumName, coverUrl, callback);
        } else {
            removeFromFavorites(context, userId, albumName, coverUrl, callback);
        }
    }

    private static void addToFavorites(Context context, String userId, String albumName, String coverUrl, FavoriteCallback callback) {
        db.collection("users")
            .document(userId)
            .update("favoriteAlbums", FieldValue.arrayUnion(albumName))
            .addOnSuccessListener(aVoid -> {
                // Broadcast the change
                broadcastFavoriteUpdate(context, albumName, true, coverUrl);
                Toast.makeText(context, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onSuccess(true);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Lỗi khi cập nhật trạng thái yêu thích", Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onError();
                }
            });
    }

    private static void removeFromFavorites(Context context, String userId, String albumName, String coverUrl, FavoriteCallback callback) {
        db.collection("users")
            .document(userId)
            .update("favoriteAlbums", FieldValue.arrayRemove(albumName))
            .addOnSuccessListener(aVoid -> {
                // Broadcast the change
                broadcastFavoriteUpdate(context, albumName, false, coverUrl);
                Toast.makeText(context, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onSuccess(false);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Lỗi khi cập nhật trạng thái yêu thích", Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onError();
                }
            });
    }

    private static void broadcastFavoriteUpdate(Context context, String albumName, boolean isFavorite, String coverUrl) {
        Intent intent = new Intent(BROADCAST_FAVORITE_UPDATED);
        intent.putExtra("albumName", albumName);
        intent.putExtra("isFavorite", isFavorite);
        intent.putExtra("coverUrl", coverUrl);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void checkFavoriteStatus(String userId, String albumName, FavoriteCallback callback) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                List<String> favoriteAlbums = (List<String>) documentSnapshot.get("favoriteAlbums");
                boolean isFavorite = favoriteAlbums != null && favoriteAlbums.contains(albumName);
                if (callback != null) {
                    callback.onSuccess(isFavorite);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onError();
                }
            });
    }
} 