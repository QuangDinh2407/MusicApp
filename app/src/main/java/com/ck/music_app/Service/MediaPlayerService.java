package com.ck.music_app.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.os.Handler;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerService extends Service {
    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();
    private List<Song> playlist;
    private int currentPosition = -1;
    private boolean isPlaying = false;
    private float currentVolume = 1.0f;
    private boolean isMuted = false;
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private FirebaseFirestore db;
    private MediaPlayerCallback callback;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private Bitmap currentAlbumArt = null;

    // Interface để callback về activity
    public interface MediaPlayerCallback {
        void onSongChanged(Song song, int position);

        void onPlayStateChanged(boolean isPlaying);
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        playlist = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        createNotificationChannel();
        setupMediaPlayer();
    }

    private void setupMediaPlayer() {
        mediaPlayer.setOnCompletionListener(mp -> {
            playNext();
        });

        // Setup progress update runnable
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && mediaPlayer != null) {
                    updateNotification();
                    progressHandler.postDelayed(this, 1000); // Update every second
                }
            }
        };
    }

    public void setCallback(MediaPlayerCallback callback) {
        this.callback = callback;
    }

    public void removeCallback() {
        this.callback = null;
    }

    private void notifyCallback() {
        if (callback != null) {
            Song currentSong = getCurrentSong();
            if (currentSong != null) {
                callback.onSongChanged(currentSong, currentPosition);
                callback.onPlayStateChanged(isPlaying);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Music Player Controls");
            channel.setShowBadge(false);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build();
            channel.setSound(null, audioAttributes);

            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        if (currentPosition < 0 || currentPosition >= playlist.size())
            return;

        Song currentSong = playlist.get(currentPosition);

        // Collapsed layout (small notification)
        RemoteViews collapsedLayout = new RemoteViews(getPackageName(), R.layout.notification_player_collapsed);

        // Expanded layout (big notification)
        RemoteViews expandedLayout = new RemoteViews(getPackageName(), R.layout.notification_player_modern);

        // Setup collapsed layout (basic info + controls)
        setupCollapsedLayout(collapsedLayout, currentSong);

        // Setup expanded layout (full info + progress + album art)
        setupExpandedLayout(expandedLayout, currentSong);

        // Lấy tên nghệ sĩ từ Firestore và build notification
        db.collection("artists")
                .document(currentSong.getArtistId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String artistName = documentSnapshot.exists() ? documentSnapshot.getString("name")
                            : "Unknown Artist";

                    // Set artist name for both layouts
                    collapsedLayout.setTextViewText(R.id.notification_artist, artistName);
                    expandedLayout.setTextViewText(R.id.notification_artist, artistName);

                    // Build notification with both layouts
                    buildNotification(collapsedLayout, expandedLayout);
                })
                .addOnFailureListener(e -> {
                    // Set default artist name and build notification
                    collapsedLayout.setTextViewText(R.id.notification_artist, "Unknown Artist");
                    expandedLayout.setTextViewText(R.id.notification_artist, "Unknown Artist");
                    buildNotification(collapsedLayout, expandedLayout);
                });

        // Initial notification build (will be updated after getting artist info)
        buildNotification(collapsedLayout, expandedLayout);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PLAY_PAUSE":
                    togglePlayPause();
                    break;
                case "NEXT":
                    playNext();
                    break;
                case "PREVIOUS":
                    playPrevious();
                    break;
                case "TOGGLE_FAVORITE":
                    toggleFavorite();
                    break;
                case "CLOSE":
                    stopForeground(true);
                    stopSelf();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    public void setPlaylist(List<Song> songs) {
        this.playlist = songs;
    }

    public void playSong(int position) {
        if (position < 0 || position >= playlist.size())
            return;

        currentPosition = position;
        Song song = playlist.get(position);

        // Clear cached album art when changing songs
        currentAlbumArt = null;

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getAudioUrl());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            updateNotification();
            notifyCallback();
            // Start progress updates
            progressHandler.post(progressRunnable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.pause();
                progressHandler.removeCallbacks(progressRunnable);
            } else {
                mediaPlayer.start();
                progressHandler.post(progressRunnable);
            }
            isPlaying = !isPlaying;
            updateNotification();
            notifyCallback();
        }
    }

    public void playNext() {
        if (playlist.isEmpty())
            return;
        int nextPosition = (currentPosition + 1) % playlist.size();
        playSong(nextPosition);
    }

    public void playPrevious() {
        if (playlist.isEmpty())
            return;
        int previousPosition = (currentPosition - 1 + playlist.size()) % playlist.size();
        playSong(previousPosition);
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            mediaPlayer.setVolume(0, 0);
        } else {
            mediaPlayer.setVolume(currentVolume, currentVolume);
        }
        updateNotification();
    }

    public void setVolume(float volume) {
        currentVolume = volume;
        if (!isMuted) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Song getCurrentSong() {
        if (currentPosition >= 0 && currentPosition < playlist.size()) {
            return playlist.get(currentPosition);
        }
        return null;
    }

    public void toggleFavorite() {
        Song currentSong = getCurrentSong();
        if (currentSong != null) {
            // TODO: Implement favorite toggle logic with Firebase
            // For now, just update notification
            updateNotification();
        }
    }

    private String formatTime(int milliseconds) {
        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void setupCollapsedLayout(RemoteViews layout, Song song) {
        // Set app logo
        layout.setImageViewResource(R.id.notification_app_logo, R.drawable.notification_app_logo);

        // Set basic song info
        layout.setTextViewText(R.id.notification_title, song.getTitle());

        // Set play/pause button
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        layout.setImageViewResource(R.id.notification_play_pause, playPauseIcon);

        // Set up button click listeners
        setupButtonListeners(layout, false); // false = no favorite button in collapsed
    }

    private void setupExpandedLayout(RemoteViews layout, Song song) {
        // Set app logo
        layout.setImageViewResource(R.id.notification_app_logo, R.drawable.notification_app_logo);

        // Set song info
        layout.setTextViewText(R.id.notification_title, song.getTitle());

        // Set play/pause button
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        layout.setImageViewResource(R.id.notification_play_pause, playPauseIcon);

        // Set progress bar
        if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
            int progress = (mediaPlayer.getCurrentPosition() * 100) / mediaPlayer.getDuration();
            layout.setProgressBar(R.id.notification_progress, 100, progress, false);

            // Set time labels
            layout.setTextViewText(R.id.notification_current_time,
                    formatTime(mediaPlayer.getCurrentPosition()));
            layout.setTextViewText(R.id.notification_total_time,
                    formatTime(mediaPlayer.getDuration()));
        } else {
            // Set default values when no media
            layout.setProgressBar(R.id.notification_progress, 100, 0, false);
            layout.setTextViewText(R.id.notification_current_time, "00:00");
            layout.setTextViewText(R.id.notification_total_time, "00:00");
        }

        // Set album art
        loadAlbumArt(layout, song);

        // Set up button click listeners
        setupButtonListeners(layout, true); // true = has favorite button in expanded
    }

    private void setupButtonListeners(RemoteViews layout, boolean hasFavorite) {
        // Create intents for buttons
        Intent playPauseIntent = new Intent(this, MediaPlayerService.class).setAction("PLAY_PAUSE");
        Intent nextIntent = new Intent(this, MediaPlayerService.class).setAction("NEXT");
        Intent previousIntent = new Intent(this, MediaPlayerService.class).setAction("PREVIOUS");
        Intent closeIntent = new Intent(this, MediaPlayerService.class).setAction("CLOSE");

        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 1, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 2, previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent closePendingIntent = PendingIntent.getService(this, 4, closeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set click listeners
        layout.setOnClickPendingIntent(R.id.notification_play_pause, playPausePendingIntent);
        layout.setOnClickPendingIntent(R.id.notification_next, nextPendingIntent);
        layout.setOnClickPendingIntent(R.id.notification_previous, previousPendingIntent);
        layout.setOnClickPendingIntent(R.id.notification_close, closePendingIntent);

        // Set favorite button only for expanded layout
        if (hasFavorite) {
            Intent favoriteIntent = new Intent(this, MediaPlayerService.class).setAction("TOGGLE_FAVORITE");
            PendingIntent favoritePendingIntent = PendingIntent.getService(this, 3, favoriteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            layout.setOnClickPendingIntent(R.id.notification_favorite, favoritePendingIntent);
        }
    }

    private void loadAlbumArt(RemoteViews layout, Song song) {
        // Use cached album art if available
        if (currentAlbumArt != null) {
            layout.setImageViewBitmap(R.id.notification_album_art, currentAlbumArt);
            return;
        }

        // Set default image first
        layout.setImageViewResource(R.id.notification_album_art, R.drawable.default_cover);

        // Load actual image if URL exists
        if (song.getCoverUrl() != null && !song.getCoverUrl().isEmpty()) {
            try {
                Glide.with(this)
                        .asBitmap()
                        .load(song.getCoverUrl())
                        .placeholder(R.drawable.default_cover)
                        .error(R.drawable.default_cover)
                        .centerCrop()
                        .override(100, 100) // Set exact size for notification
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // Cache for
                                                                                                 // performance
                        .into(new CustomTarget<Bitmap>(100, 100) {
                            @Override
                            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                // Cache the loaded bitmap
                                currentAlbumArt = resource;

                                // Set the loaded bitmap to RemoteViews
                                layout.setImageViewBitmap(R.id.notification_album_art, resource);

                                // Rebuild notification with updated image
                                Song currentSong = getCurrentSong();
                                if (currentSong != null) {
                                    // Create fresh layouts with the new image
                                    RemoteViews collapsedLayout = new RemoteViews(getPackageName(),
                                            R.layout.notification_player_collapsed);
                                    RemoteViews expandedLayout = new RemoteViews(getPackageName(),
                                            R.layout.notification_player_modern);

                                    setupCollapsedLayout(collapsedLayout, currentSong);
                                    setupExpandedLayout(expandedLayout, currentSong);

                                    // Get artist name and build notification
                                    db.collection("artists")
                                            .document(currentSong.getArtistId())
                                            .get()
                                            .addOnSuccessListener(documentSnapshot -> {
                                                String artistName = documentSnapshot.exists()
                                                        ? documentSnapshot.getString("name")
                                                        : "Unknown Artist";

                                                collapsedLayout.setTextViewText(R.id.notification_artist, artistName);
                                                expandedLayout.setTextViewText(R.id.notification_artist, artistName);

                                                buildNotification(collapsedLayout, expandedLayout);
                                            })
                                            .addOnFailureListener(e -> {
                                                collapsedLayout.setTextViewText(R.id.notification_artist,
                                                        "Unknown Artist");
                                                expandedLayout.setTextViewText(R.id.notification_artist,
                                                        "Unknown Artist");
                                                buildNotification(collapsedLayout, expandedLayout);
                                            });
                                }
                            }

                            @Override
                            public void onLoadCleared(Drawable placeholder) {
                                // Clear cache when cleared
                                currentAlbumArt = null;
                            }

                            @Override
                            public void onLoadFailed(Drawable errorDrawable) {
                                Log.d("MediaPlayerService", "Failed to load album art: " + song.getCoverUrl());
                                // Keep default image on failure
                            }
                        });
            } catch (Exception e) {
                Log.e("MediaPlayerService", "Error loading album art", e);
                // Keep default image on exception
            }
        }
    }

    private void buildNotification(RemoteViews collapsedLayout, RemoteViews expandedLayout) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setCustomContentView(collapsedLayout) // Collapsed state
                .setCustomBigContentView(expandedLayout) // Expanded state
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setOngoing(true)
                .build();

        if (isPlaying) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (progressHandler != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }
}