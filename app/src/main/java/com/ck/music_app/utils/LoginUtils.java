package com.ck.music_app.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.Services.FirebaseService;
import com.ck.music_app.Services.MusicService;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class LoginUtils {
    private static final String TAG = "LoginUtils";

    public static void handleLoginSuccess(Context context, FirebaseUser user, String email) {
        if (user != null) {
            // Kiểm tra xem có đang phát nhạc không
            Song currentPlayingSong = MusicService.getCurrentSongStatic();

            
            if (currentPlayingSong != null) {
                // Nếu đang phát nhạc, chuyển sang MainActivity với thông tin bài hát đang phát
                Intent intent = new Intent(context, MainActivity.class);

                // Lấy playlist hiện tại từ MusicService
                List<Song> currentPlaylist = MusicService.getCurrentPlaylist();
                if (currentPlaylist == null) {
                    currentPlaylist = new ArrayList<>();
                    currentPlaylist.add(currentPlayingSong);
                }

                // Lưu trạng thái phát nhạc hiện tại
                boolean isPlaying = MusicService.isPlaying();
                int currentPosition = MusicService.getCurrentPosition();

                // Truyền thông tin bài hát và playlist
                intent.putExtra("current_song", currentPlayingSong);
                intent.putExtra("songList", new ArrayList<>(currentPlaylist));
                intent.putExtra("position", MusicService.getCurrentIndex());
                intent.putExtra("showPlayer", true);
                intent.putExtra("load_song_from_login", true);
                intent.putExtra("resume_playback", isPlaying);
                intent.putExtra("playback_position", currentPosition);
                System.out.println("1");
                context.startActivity(intent);
                if (!(context instanceof MainActivity)) {
                    ((android.app.Activity) context).finish();
                }
            } else {
                System.out.println("2");
                // Nếu không phát nhạc, load bài hát từ dữ liệu
                checkUserCurrentSong(context, user);
            }
        } else {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("email", email);
            context.startActivity(intent);
            if (!(context instanceof MainActivity)) {
                ((android.app.Activity) context).finish();
            }
        }
    }

    private static void checkUserCurrentSong(Context context, FirebaseUser user) {
        FirebaseService.getInstance().getUserCurrentSong(user.getUid(), new FirebaseService.FirestoreCallback<Song>() {
            @Override
            public void onSuccess(Song song) {
                // Chuyển sang MainActivity với thông tin bài hát
                Intent intent = new Intent(context, MainActivity.class);
                if (song != null) {
                    intent.putExtra("current_song", song);
                    intent.putExtra("load_song_from_login", true);
                }
                context.startActivity(intent);
                if (!(context instanceof MainActivity)) {
                    ((android.app.Activity) context).finish();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, "Error getting user's current song", e);
                // Vẫn chuyển sang MainActivity nếu có lỗi
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                if (!(context instanceof MainActivity)) {
                    ((android.app.Activity) context).finish();
                }
            }
        });
    }
} 