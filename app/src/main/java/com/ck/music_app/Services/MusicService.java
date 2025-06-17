package com.ck.music_app.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.utils.MusicUtils;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    public static final String ACTION_PLAY = "com.ck.music_app.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.ck.music_app.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.ck.music_app.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.ck.music_app.ACTION_NEXT";
    public static final String ACTION_STOP = "com.ck.music_app.ACTION_STOP";
    public static final String ACTION_SEEK = "com.ck.music_app.ACTION_SEEK";
    public static final String ACTION_MUTE = "com.ck.music_app.ACTION_MUTE";

    // Notification constants
    public static final String NOTIFICATION_CHANNEL_ID = "music_player_channel";
    public static final int NOTIFICATION_ID = 1;

    // Broadcast actions
    public static final String BROADCAST_PLAYING_STATE = "com.ck.music_app.PLAYING_STATE";
    public static final String BROADCAST_SONG_CHANGED = "com.ck.music_app.SONG_CHANGED";
    public static final String BROADCAST_PROGRESS = "com.ck.music_app.PROGRESS";
    public static final String BROADCAST_LOADING_STATE = "com.ck.music_app.LOADING_STATE";
    public static final String BROADCAST_LYRIC_POSITION = "com.ck.music_app.LYRIC_POSITION";

    private MediaPlayer mediaPlayer;
    private List<Song> songList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private float volumeBeforeMute = 1.0f;
    private LocalBroadcastManager broadcaster;
    private Handler lyricHandler;
    private static final int LYRIC_UPDATE_INTERVAL = 100; // 100ms

    // Notification related
    private NotificationManager notificationManager;
    private MediaSessionCompat mediaSession;
    private int currentProgress = 0;
    private int currentDuration = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
        mediaPlayer = new MediaPlayer();
        lyricHandler = new Handler(Looper.getMainLooper());

        // Initialize notification manager
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();

        // Initialize MediaSession
        mediaSession = new MediaSessionCompat(this, "MusicService");

        mediaPlayer.setOnCompletionListener(mp -> {
            // Auto play next song when current song completes
            if (currentIndex < songList.size() - 1) {
                playNext();
            } else {
                stopSelf();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    if (intent.hasExtra("songList") && intent.hasExtra("position")) {
                        songList = (List<Song>) intent.getSerializableExtra("songList");
                        currentIndex = intent.getIntExtra("position", 0);
                        playSong(currentIndex);
                    } else {
                        resumeMusic();
                    }
                    break;
                case ACTION_PAUSE:
                    pauseMusic();
                    break;
                case ACTION_PREVIOUS:
                    playPrevious();
                    break;
                case ACTION_NEXT:
                    playNext();
                    break;
                case ACTION_STOP:
                    stopSelf();
                    break;
                case ACTION_SEEK:
                    int position = intent.getIntExtra("position", 0);
                    seekTo(position);
                    break;
                case ACTION_MUTE:
                    toggleMute();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    private void playSong(int index) {
        if (index < 0 || index >= songList.size())
            return;

        try {
            mediaPlayer.reset();
            Song song = songList.get(index);
            broadcastLoadingState(true);
            mediaPlayer.setDataSource(song.getAudioUrl());
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                isPlaying = true;
                // Initialize progress values
                currentProgress = 0;
                currentDuration = mediaPlayer.getDuration();

                // Start updates and broadcast state
                startProgressUpdates();
                broadcastPlayingState(true);
                broadcastSongChanged(currentIndex);
                broadcastLoadingState(false);

                // Immediately broadcast initial progress to update notification
                broadcastProgress(currentProgress, currentDuration);

                // Start foreground service
                startForeground(NOTIFICATION_ID, createNotification());
            });

        } catch (Exception e) {
            e.printStackTrace();
            broadcastLoadingState(false);
        }
    }

    private void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            // Update current progress when resuming
            if (currentDuration == 0) {
                currentDuration = mediaPlayer.getDuration();
            }
            currentProgress = mediaPlayer.getCurrentPosition();
            startProgressUpdates();
            startLyricUpdates();
            broadcastPlayingState(true);
            showNotification();
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            stopLyricUpdates();
            broadcastPlayingState(false);
            showNotification();
        }
    }

    private void playPrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            // Reset progress values for new song
            currentProgress = 0;
            currentDuration = 0;
            playSong(currentIndex);
        }
    }

    private void playNext() {
        if (currentIndex < songList.size() - 1) {
            currentIndex++;
            // Reset progress values for new song
            currentProgress = 0;
            currentDuration = 0;
            playSong(currentIndex);
        }
    }

    private void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    private void toggleMute() {
        if (mediaPlayer != null) {
            if (isMuted) {
                // Unmute
                mediaPlayer.setVolume(volumeBeforeMute, volumeBeforeMute);
                isMuted = false;
            } else {
                // Mute
                volumeBeforeMute = 1.0f; // Store current volume
                mediaPlayer.setVolume(0.0f, 0.0f);
                isMuted = true;
            }
            showNotification(); // Update notification to reflect mute state
        }
    }

    private void startProgressUpdates() {
        // Cập nhật progress bar mỗi giây
        new Thread(() -> {
            while (isPlaying && mediaPlayer != null) {
                try {
                    Thread.sleep(1000);
                    if (mediaPlayer.isPlaying()) {
                        broadcastProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Cập nhật vị trí lyrics mỗi 100ms
        startLyricUpdates();
    }

    private final Runnable lyricUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                broadcastLyricPosition(mediaPlayer.getCurrentPosition());
                lyricHandler.postDelayed(this, LYRIC_UPDATE_INTERVAL);
            }
        }
    };

    private void startLyricUpdates() {
        lyricHandler.removeCallbacks(lyricUpdateRunnable);
        lyricHandler.post(lyricUpdateRunnable);
    }

    private void stopLyricUpdates() {
        lyricHandler.removeCallbacks(lyricUpdateRunnable);
    }

    private void broadcastLyricPosition(int position) {
        Intent intent = new Intent(BROADCAST_LYRIC_POSITION);
        intent.putExtra("position", position);
        broadcaster.sendBroadcast(intent);
    }

    private void broadcastPlayingState(boolean playing) {
        Intent intent = new Intent(BROADCAST_PLAYING_STATE);
        intent.putExtra("isPlaying", playing);
        broadcaster.sendBroadcast(intent);
    }

    private void broadcastSongChanged(int position) {
        Intent intent = new Intent(BROADCAST_SONG_CHANGED);
        intent.putExtra("position", position);
        broadcaster.sendBroadcast(intent);
    }

    private void broadcastProgress(int progress, int duration) {
        // Update stored progress values
        boolean shouldUpdateNotification = (currentProgress / 1000) != (progress / 1000); // Only update when seconds
                                                                                          // change
        currentProgress = progress;
        currentDuration = duration;

        Intent intent = new Intent(BROADCAST_PROGRESS);
        intent.putExtra("progress", progress);
        intent.putExtra("duration", duration);
        broadcaster.sendBroadcast(intent);

        // Update notification with current progress for real-time display
        // Force update every 2 seconds for visible progress
        if (shouldUpdateNotification && currentProgress % 2000 < 1000) {
            android.util.Log.d("MusicService",
                    "Updating notification - Progress: " + currentProgress + "/" + currentDuration);
            showNotification();
        }
    }

    private void broadcastLoadingState(boolean isLoading) {
        Intent intent = new Intent(BROADCAST_LOADING_STATE);
        intent.putExtra("isLoading", isLoading);
        broadcaster.sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopLyricUpdates();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Music Player Controls");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Song currentSong = getCurrentSong();
        if (currentSong == null)
            return null;

        // Create intent for opening the app
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create action intents
        Intent playPauseIntent = new Intent(this, MusicService.class);
        playPauseIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this, 1, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction(ACTION_PREVIOUS);
        PendingIntent previousPendingIntent = PendingIntent.getService(
                this, 2, previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(
                this, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MusicService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 4, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent muteIntent = new Intent(this, MusicService.class);
        muteIntent.setAction(ACTION_MUTE);
        PendingIntent mutePendingIntent = PendingIntent.getService(
                this, 5, muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build notification with MediaStyle and progress
        String progressText = formatTime(currentProgress) + " / " + formatTime(currentDuration);
        String statusText = "🎵 Music Player - " + (isPlaying ? "Playing" : "Paused") + (isMuted ? " • Muted" : "");

        // Calculate progress percentage and create visual progress bar
        int progressPercent = 0;
        String visualProgressBar = "";
        if (currentDuration > 0) {
            progressPercent = (int) ((currentProgress * 100L) / currentDuration);
            // Create visual progress bar with Unicode blocks
            int filledBlocks = progressPercent / 5; // 20 blocks total (100/5)
            StringBuilder progressBar = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                if (i < filledBlocks) {
                    progressBar.append("█");
                } else {
                    progressBar.append("░");
                }
            }
            visualProgressBar = progressBar.toString();
        } else {
            visualProgressBar = "░░░░░░░░░░░░░░░░░░░░";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_new)
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtistId())
                .setSubText(progressText + " " + visualProgressBar + " " + progressPercent + "%")
                .setContentIntent(pendingIntent)
                .setDeleteIntent(stopPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setShowWhen(false)
                .setColor(getResources().getColor(R.color.colorPrimary, null))
                .setOngoing(isPlaying)
                .addAction(R.drawable.ic_skip_previous_white_36dp, "Previous", previousPendingIntent)
                .addAction(
                        isPlaying ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_white_36dp,
                        isPlaying ? "Pause" : "Play",
                        playPausePendingIntent)
                .addAction(R.drawable.ic_skip_next_white_36dp, "Next", nextPendingIntent)
                .addAction(
                        isMuted ? R.drawable.volume_mute : R.drawable.volume_high,
                        isMuted ? "Unmute" : "Mute",
                        mutePendingIntent)
                .addAction(R.drawable.ic_cancel, "Close", stopPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2, 3)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopPendingIntent));

        // Load album art asynchronously
        loadAlbumArt(builder, currentSong.getCoverUrl());

        return builder.build();
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void loadAlbumArt(NotificationCompat.Builder builder, String imageUrl) {
        // First, show notification with default album art
        Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art);
        builder.setLargeIcon(defaultBitmap);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        // Then, load actual album art asynchronously
        try {
            Glide.with(this)
                    .asBitmap()
                    .load(imageUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            builder.setLargeIcon(resource);
                            notificationManager.notify(NOTIFICATION_ID, builder.build());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Keep default album art
                        }
                    });
        } catch (Exception e) {
            // Already using default album art
        }
    }

    private void showNotification() {
        try {
            // Cancel existing notification to force refresh
            notificationManager.cancel(NOTIFICATION_ID);

            // Create and show new notification
            Notification notification = createNotification();
            if (notification != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
                android.util.Log.d("MusicService", "Notification updated with progress: " +
                        (currentDuration > 0 ? (currentProgress * 100 / currentDuration) : 0) + "%");
            }
        } catch (Exception e) {
            android.util.Log.e("MusicService", "Error updating notification", e);
        }
    }

    private Song getCurrentSong() {
        if (songList != null && currentIndex >= 0 && currentIndex < songList.size()) {
            return songList.get(currentIndex);
        }
        return null;
    }
}