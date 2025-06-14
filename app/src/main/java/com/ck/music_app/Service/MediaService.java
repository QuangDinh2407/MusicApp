package com.ck.music_app.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MediaService extends Service {
    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_PLAY = "com.ck.music_app.ACTION_PLAY";
    private static final String ACTION_PAUSE = "com.ck.music_app.ACTION_PAUSE";
    private static final String ACTION_NEXT = "com.ck.music_app.ACTION_NEXT";
    private static final String ACTION_PREVIOUS = "com.ck.music_app.ACTION_PREVIOUS";
    private static final String ACTION_CLOSE = "com.ck.music_app.ACTION_CLOSE";

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private List<Song> playlist;
    private int currentSongIndex = -1;
    private boolean isPlaying = false;
    private MediaSessionCompat mediaSession;
    private NotificationManager notificationManager;

    public class LocalBinder extends Binder {
        public MediaService getService() {
            return MediaService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        playlist = new ArrayList<>();
        mediaPlayer = new MediaPlayer();
        mediaSession = new MediaSessionCompat(this, "MusicService");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    play();
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
                case ACTION_NEXT:
                    playNext();
                    break;
                case ACTION_PREVIOUS:
                    playPrevious();
                    break;
                case ACTION_CLOSE:
                    stopForeground(true);
                    stopSelf();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Music player controls");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build();
            channel.setSound(null, audioAttributes);

            notificationManager.createNotificationChannel(channel);
        }
    }

    private PendingIntent createPendingIntent(String action) {
        Intent intent = new Intent(this, MediaService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateNotification() {
        if (currentSongIndex < 0 || currentSongIndex >= playlist.size()) {
            return;
        }

        Song currentSong = playlist.get(currentSongIndex);

        // Tạo intent để mở MainActivity khi click vào notification
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tạo các action cho notification
        PendingIntent playPauseIntent = createPendingIntent(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent nextIntent = createPendingIntent(ACTION_NEXT);
        PendingIntent previousIntent = createPendingIntent(ACTION_PREVIOUS);
        PendingIntent closeIntent = createPendingIntent(ACTION_CLOSE);

        // Tải ảnh bìa album
        Bitmap coverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_cover);
        if (currentSong.getCoverUrl() != null && !currentSong.getCoverUrl().isEmpty()) {
            try {
                FutureTarget<Bitmap> futureTarget = Glide.with(this)
                        .asBitmap()
                        .load(currentSong.getCoverUrl())
                        .error(R.drawable.default_cover)
                        .placeholder(R.drawable.default_cover)
                        .submit();
                coverBitmap = futureTarget.get();
            } catch (Exception e) {
                Log.e("MediaService", "Error loading cover image", e);
            }
        }

        // Tạo notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(currentSong.getTitle())
                .setContentText("Đang phát")
                .setLargeIcon(coverBitmap)
                .setContentIntent(openAppPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying ? "Pause" : "Play", playPauseIntent)
                .addAction(R.drawable.ic_skip_previous, "Previous", previousIntent)
                .addAction(R.drawable.ic_skip_next, "Next", nextIntent)
                .addAction(R.drawable.ic_close, "Close", closeIntent)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    public void setPlaylist(List<Song> songs) {
        playlist.clear();
        playlist.addAll(songs);
    }

    public void playSong(int position) {
        if (position < 0 || position >= playlist.size()) {
            return;
        }

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(playlist.get(position).getAudioUrl());
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentSongIndex = position;
            isPlaying = true;
            updateNotification();
        } catch (IOException e) {
            Log.e("MediaService", "Error playing song", e);
        }
    }

    public void play() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer.start();
            isPlaying = true;
            updateNotification();
        }
    }

    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            updateNotification();
        }
    }

    public void playNext() {
        if (currentSongIndex < playlist.size() - 1) {
            playSong(currentSongIndex + 1);
        }
    }

    public void playPrevious() {
        if (currentSongIndex > 0) {
            playSong(currentSongIndex - 1);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
    }
}