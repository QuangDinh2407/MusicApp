package com.ck.music_app.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.List;

public class MusicService extends Service {
    private static final String TAG = "MusicService";

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

    public static final String ACTION_TOGGLE_SHUFFLE = "com.ck.music_app.action.TOGGLE_SHUFFLE";
    public static final String ACTION_TOGGLE_REPEAT = "com.ck.music_app.action.TOGGLE_REPEAT";

    // Broadcast actions
    public static final String BROADCAST_PLAYING_STATE = "com.ck.music_app.PLAYING_STATE";
    public static final String BROADCAST_SONG_CHANGED = "com.ck.music_app.SONG_CHANGED";
    public static final String BROADCAST_PROGRESS = "com.ck.music_app.PROGRESS";
    public static final String BROADCAST_LOADING_STATE = "com.ck.music_app.LOADING_STATE";
    public static final String BROADCAST_LYRIC_POSITION = "com.ck.music_app.LYRIC_POSITION";
    public static final String BROADCAST_PLAYLIST_CHANGED = "com.ck.music_app.PLAYLIST_CHANGED";

    private MediaPlayer mediaPlayer;
    private static List<Song> songList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isPlaying = false;
    private LocalBroadcastManager broadcaster;
    private Handler lyricHandler;
    private static final int LYRIC_UPDATE_INTERVAL = 100; // 100ms
    private static Song currentSong;

    private boolean isShuffleOn = false;
    private int repeatMode = 0; // 0: no repeat, 1: repeat all, 2: repeat one
    private List<Song> originalSongList = new ArrayList<>(); // Lưu danh sách gốc

    // Missing variable declarations
    private NotificationManager notificationManager;
    private MediaSessionCompat mediaSession;
    private int currentProgress = 0;
    private int currentDuration = 0;
    private boolean isMuted = false;

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
        initializeMediaPlayer();
        lyricHandler = new Handler(Looper.getMainLooper());
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(mp -> {
                // Auto play next song when current song completes
                if (currentIndex < songList.size() - 1) {
                    playNext();
                } else {
                    stopSelf();
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                broadcastLoadingState(false);
                return false;
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent != null && intent.getAction() != null) {
                switch (intent.getAction()) {
                    case ACTION_PLAY:
                        try {
                            if (intent.hasExtra("songList") && intent.hasExtra("position")) {
                                List<Song> newSongList = (List<Song>) intent.getSerializableExtra("songList");
                                int newPosition = intent.getIntExtra("position", 0);

                                // Validate songList
                                if (newSongList == null || newSongList.isEmpty()) {
                                    Log.e(TAG, "Received null or empty song list");
                                    break;
                                }

                                // Validate position
                                if (newPosition < 0 || newPosition >= newSongList.size()) {
                                    Log.e(TAG,
                                            "Invalid position: " + newPosition + ", list size: " + newSongList.size());
                                    break;
                                }

                                // Validate song data
                                Song songToPlay = newSongList.get(newPosition);
                                if (songToPlay == null || songToPlay.getAudioUrl() == null
                                        || songToPlay.getAudioUrl().trim().isEmpty()) {
                                    Log.e(TAG, "Invalid song data or empty audio URL");
                                    break;
                                }

                                // Update song list and play
                                songList = new ArrayList<>(newSongList);
                                currentIndex = newPosition;
                                playSong(currentIndex);
                            } else {
                                resumeMusic();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling ACTION_PLAY: " + e.getMessage(), e);
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
                    case ACTION_TOGGLE_SHUFFLE:
                        boolean isShuffleOn = intent.getBooleanExtra("isShuffleOn", false);
                        handleShuffleMode(isShuffleOn);
                        break;
                    case ACTION_TOGGLE_REPEAT:
                        int repeatMode = intent.getIntExtra("repeatMode", 0);
                        handleRepeatMode(repeatMode);
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in onStartCommand: " + e.getMessage(), e);
        }
        return START_NOT_STICKY;
    }

    private void playSong(int index) {
        if (index < 0 || index >= songList.size()) {
            return;
        }

        try {
            // Đảm bảo MediaPlayer được khởi tạo
            initializeMediaPlayer();

            // Reset và chuẩn bị MediaPlayer
            mediaPlayer.reset();
            Song song = songList.get(index);
            currentSong = song;
            currentIndex = index;
            broadcastLoadingState(true);
            broadcastSongChanged(currentIndex);

            try {
                // Kiểm tra xem URL có phải là local URI không
                if (song.getAudioUrl().startsWith("content://")) {
                    // Sử dụng ContentResolver để lấy FileDescriptor cho local files
                    Uri uri = Uri.parse(song.getAudioUrl());
                    AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(uri, "r");
                    if (afd != null) {
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                    }
                } else {
                    // Sử dụng URL trực tiếp cho online files
                    mediaPlayer.setDataSource(song.getAudioUrl());
                }

                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mp -> {
                    try {
                        mediaPlayer.start();
                        isPlaying = true;
                        // Initialize progress values
                        currentProgress = 0;
                        currentDuration = mediaPlayer.getDuration();

                        // Start updates and broadcast state
                        startProgressUpdates();
                        broadcastPlayingState(true);

                        // Immediately broadcast initial progress to update notification
                        broadcastProgress(currentProgress, currentDuration);

                        // Start foreground service
                        startForeground(NOTIFICATION_ID, createNotification());
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting playback: " + e.getMessage());
                    } finally {
                        broadcastLoadingState(false);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error setting data source: " + e.getMessage());
                broadcastLoadingState(false);
                Toast.makeText(this, "Không thể phát bài hát này", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in playSong: " + e.getMessage());
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
        }
    }

    public void playNext() {
        if (songList.isEmpty())
            return;

        if (repeatMode == 2) {
            // Repeat One: phát lại bài hiện tại
            playSong(currentIndex);
            return;
        }

        currentIndex++;

        // Kiểm tra nếu đã hết danh sách
        if (currentIndex >= songList.size()) {
            if (repeatMode == 1) {
                // Repeat All: quay lại bài đầu tiên
                currentIndex = 0;
            } else {
                System.out.println("stop");
                // No Repeat: dừng phát nhạc
                currentIndex = songList.size() - 1;
                pauseMusic();
                seekTo(0);
                return;
            }
        }

        playSong(currentIndex);

    }

    public void playPrevious() {
        if (songList.isEmpty())
            return;

        if (repeatMode == 2) {
            // Repeat One: phát lại bài hiện tại
            playSong(currentIndex);
            return;
        }

        currentIndex--;

        // Kiểm tra nếu đã về đầu danh sách
        if (currentIndex < 0) {
            if (repeatMode == 1) {
                // Repeat All: chuyển đến bài cuối cùng
                currentIndex = songList.size() - 1;
            } else {
                // No Repeat: ở lại bài đầu tiên
                currentIndex = 0;
                return;
            }
        }

        playSong(currentIndex);
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
                mediaPlayer.setVolume(1.0f, 1.0f);
                isMuted = false;
            } else {
                // Mute
                mediaPlayer.setVolume(0.0f, 0.0f);
                isMuted = true;
            }
            // Update notification to reflect mute state
            showNotification();
        }
    }

    private void handleShuffleMode(boolean isShuffleOn) {
        this.isShuffleOn = isShuffleOn;

        Song currentSong = songList.get(currentIndex);

        if (isShuffleOn) {
            // Lưu danh sách gốc nếu chưa có
            if (originalSongList.isEmpty()) {
                originalSongList = new ArrayList<>(songList);
            }

            // Tạo và xáo trộn danh sách mới
            List<Song> shuffledList = new ArrayList<>(songList);
            Collections.shuffle(shuffledList);

            // Đảm bảo bài hát hiện tại vẫn ở vị trí currentIndex
            shuffledList.remove(currentSong);
            shuffledList.add(currentIndex, currentSong);

            // Cập nhật danh sách phát
            songList = shuffledList;
        } else {
            // Khôi phục lại danh sách gốc
            if (!originalSongList.isEmpty()) {
                songList = new ArrayList<>(originalSongList);
                // Tìm vị trí mới của bài hát hiện tại trong danh sách gốc
                currentIndex = songList.indexOf(currentSong);
            }
        }

        // Broadcast playlist changed
        broadcastPlaylistChanged();

    }

    private void handleRepeatMode(int repeatMode) {
        this.repeatMode = repeatMode;

        // Cập nhật MediaPlayer completion listener dựa trên chế độ lặp lại
        mediaPlayer.setOnCompletionListener(mp -> {
            switch (repeatMode) {
                case 0: // Không lặp lại
                    if (currentIndex < songList.size() - 1) {
                        playNext();
                    } else {
                        // Dừng phát nhạc khi hết danh sách
                        pauseMusic();
                        seekTo(0);
                    }
                    break;

                case 1: // Lặp lại tất cả
                    playNext();
                    break;

                case 2: // Lặp lại một bài
                    // Phát lại bài hiện tại
                    playSong(currentIndex);
                    break;
            }
        });
    }

    private void startProgressUpdates() {
        // Cập nhật progress bar mỗi giây
        new Thread(() -> {
            try {
                while (isPlaying && mediaPlayer != null) {
                    try {
                        if (mediaPlayer.isPlaying()) {
                            int currentPos = mediaPlayer.getCurrentPosition();
                            int duration = mediaPlayer.getDuration();
                            broadcastProgress(currentPos, duration);
                        }
                        Thread.sleep(1000);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "MediaPlayer in illegal state: " + e.getMessage());
                        break;
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Progress update thread interrupted");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        Log.e(TAG, "Error in progress updates: " + e.getMessage());
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Fatal error in progress thread: " + e.getMessage());
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

    private void broadcastPlaylistChanged() {
        Intent intent = new Intent(BROADCAST_PLAYLIST_CHANGED);
        intent.putExtra("songList", new ArrayList<>(songList));
        intent.putExtra("currentIndex", currentIndex);
        intent.putExtra("isShuffleOn", isShuffleOn);
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

        // Stop all updates first
        stopLyricUpdates();

        // Stop progress updates by setting isPlaying to false
        isPlaying = false;

        // Release MediaPlayer safely
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaPlayer: " + e.getMessage());
            } finally {
                mediaPlayer = null;
            }
        }

        // Release MediaSession safely
        if (mediaSession != null) {
            try {
                mediaSession.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaSession: " + e.getMessage());
            } finally {
                mediaSession = null;
            }
        }

        // Clear static references to prevent memory leaks
        currentSong = null;
        if (songList != null) {
            songList.clear();
        }
        if (originalSongList != null) {
            originalSongList.clear();
        }

        // Stop foreground service
        try {
            stopForeground(true);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping foreground: " + e.getMessage());
        }

        // Cancel all notifications
        if (notificationManager != null) {
            try {
                notificationManager.cancel(NOTIFICATION_ID);
            } catch (Exception e) {
                Log.e(TAG, "Error canceling notification: " + e.getMessage());
            }
        }

        Log.d(TAG, "MusicService destroyed successfully");
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

    // Add static methods for external access
    public static Song getCurrentSongStatic() {
        return currentSong;
    }

    public static List<Song> getCurrentPlaylist() {
        return new ArrayList<>(songList);
    }

    public static boolean isServicePlaying() {
        // This should be called from a service instance, but we'll provide a static
        // method
        // You should maintain a static reference or use a different approach
        return false; // Default value, implement proper logic based on your needs
    }
}
