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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    public static final String ACTION_RESUME = "com.ck.music_app.ACTION_RESUME";
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
    private static int currentIndex = 0;
    private boolean isPlaying = false;
    private boolean isLoading = false;
    private LocalBroadcastManager broadcaster;
    private Handler lyricHandler;
    private static final int LYRIC_UPDATE_INTERVAL = 100; // 100ms
    private static Song currentSong;

    private boolean isShuffleOn = false;
    private int repeatMode = 0; // 0: no repeat, 1: repeat all, 2: repeat one
    private List<Song> originalSongList = new ArrayList<>(); // Lưu danh sách gốc

    private static MusicService instance;

    // Missing variable declarations
    private NotificationManager notificationManager;
    private MediaSessionCompat mediaSession;
    private int currentProgress = 0;
    private int currentDuration = 0;
    private boolean isMuted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        broadcaster = LocalBroadcastManager.getInstance(this);
        lyricHandler = new Handler(Looper.getMainLooper());

        // Initialize notification manager
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();

        // Initialize MediaSession
        mediaSession = new MediaSessionCompat(this, "MusicService");

        // Initialize MediaPlayer first
        initializeMediaPlayer();
        lyricHandler = new Handler(Looper.getMainLooper());

    }

    private void initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                isLoading = false;
                broadcastLoadingState(false);
                return false;
            });
            
            // Thêm OnCompletionListener để tự động phát bài tiếp theo khi bài hát kết thúc
            mediaPlayer.setOnCompletionListener(mp -> {
                if (!isLoading && mediaPlayer.getCurrentPosition() > 1000) {
                    playNext();
                }
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            try {
                switch (intent.getAction()) {
                    case ACTION_PLAY:
                        if (intent.hasExtra("songList") && intent.hasExtra("position")) {
                            List<Song> newSongList = (List<Song>) intent.getSerializableExtra("songList");
                            int position = intent.getIntExtra("position", 0);

                            // Validate songList
                            if (newSongList == null || newSongList.isEmpty()) {
                                Log.e(TAG, "Received null or empty song list");
                                break;
                            }

                            // Validate position
                            if (position < 0 || position >= newSongList.size()) {
                                Log.e(TAG, "Invalid position: " + position + ", list size: " + newSongList.size());
                                break;
                            }

                            // Validate song data
                            Song songToPlay = newSongList.get(position);
                            if (songToPlay == null || songToPlay.getAudioUrl() == null
                                    || songToPlay.getAudioUrl().trim().isEmpty()) {
                                Log.e(TAG, "Invalid song data or empty audio URL");
                                break;
                            }

                            // Lưu danh sách gốc mới
                            originalSongList = new ArrayList<>(newSongList);

                            // Nếu đang bật shuffle, xử lý shuffle
                            if (isShuffleOn) {
                                songList = new ArrayList<>();
                                songList.add(newSongList.get(position));
                                List<Song> remainingSongs = new ArrayList<>(newSongList);
                                remainingSongs.remove(position);
                                Collections.shuffle(remainingSongs);
                                songList.addAll(remainingSongs);
                                currentIndex = 0;
                            } else {
                                songList = new ArrayList<>(newSongList);
                                currentIndex = position;
                            }

                            // Kiểm tra xem có phải bài hát hiện tại
                            boolean isSameSong = false;
                            if (currentSong != null && currentIndex < songList.size()) {
                                Song newSong = songList.get(currentIndex);
                                if (currentSong.getSongId() != null && newSong.getSongId() != null) {
                                    isSameSong = currentSong.getSongId().equals(newSong.getSongId());
                                }
                            }

                            if (isSameSong && mediaPlayer != null && !mediaPlayer.isPlaying()) {
                                resumeMusic();
                            } else {
                                playSong(currentIndex);
                            }
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

                    case ACTION_RESUME:
                        resumeMusic();
                        break;

                    case ACTION_MUTE:
                        toggleMute();
                        break;

                    case ACTION_TOGGLE_SHUFFLE:
                        isShuffleOn = intent.getBooleanExtra("isShuffleOn", false);
                        handleShuffleMode();
                        break;

                    case ACTION_TOGGLE_REPEAT:
                        int repeatMode = intent.getIntExtra("repeatMode", 0);
                        handleRepeatMode(repeatMode);
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in onStartCommand: " + e.getMessage(), e);
            }
            return START_NOT_STICKY;
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
            isLoading = true;
            broadcastLoadingState(true);
            broadcastSongChanged(currentIndex);

            // Cập nhật songPlaying lên Firebase (chỉ với bài hát online)
            FirebaseService.getInstance().updateCurrentPlayingSong(song);
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
                        isLoading = false;
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
                isLoading = false;
                broadcastLoadingState(false);
                Toast.makeText(this, "Không thể phát bài hát này", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in playSong: " + e.getMessage());
            isLoading = false;
            broadcastLoadingState(false);
        }
    }

    private void resumeMusic() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.start();
                isPlaying = true;
                startProgressUpdates();
                startLyricUpdates();
                broadcastPlayingState(true);
                showNotification();
                System.out.println("Music resumed successfully");
            } catch (IllegalStateException e) {
                e.printStackTrace();
                // Thử phát lại từ đầu nếu không thể resume
                if (!songList.isEmpty()) {
                    playSong(currentIndex);
                }
            }
        } else {
            // Thử khởi tạo lại MediaPlayer nếu nó là null
            if (!songList.isEmpty()) {
                playSong(currentIndex);
            }
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause();
                isPlaying = false;
                stopLyricUpdates();
                broadcastPlayingState(false);
                showNotification(); // Cập nhật notification để đổi nút
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
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

    private void handleShuffleMode() {
        if (isShuffleOn) {
            // Lấy bài hát hiện tại
            System.out.println("hahaha");
            Song currentSong = songList.get(currentIndex);

            // Tạo danh sách shuffle mới
            songList = new ArrayList<>();
            songList.add(currentSong); // Đặt bài hát hiện tại lên đầu

            // Shuffle phần còn lại của danh sách
            List<Song> remainingSongs = new ArrayList<>(originalSongList);
            remainingSongs.remove(originalSongList.indexOf(currentSong));
            Collections.shuffle(remainingSongs);
            songList.addAll(remainingSongs);

            currentIndex = 0; // Vì bài hát hiện tại luôn ở vị trí đầu
        } else {
            // Khôi phục lại danh sách gốc
            if (!originalSongList.isEmpty()) {
                // Lấy bài hát hiện tại
                Song currentSong = songList.get(currentIndex);
                // Tìm vị trí của bài hát trong danh sách gốc
                int originalIndex = -1;
                for (int i = 0; i < originalSongList.size(); i++) {
                    if (originalSongList.get(i).getSongId().equals(currentSong.getSongId())) {
                        originalIndex = i;
                        break;
                    }
                }
                // Nếu không tìm thấy, giữ nguyên vị trí hiện tại
                if (originalIndex == -1) {
                    originalIndex = Math.min(currentIndex, originalSongList.size() - 1);
                }
                songList = new ArrayList<>(originalSongList);
                currentIndex = originalIndex;
            }
        }
        broadcastPlaylistChanged();
    }

    private void handleRepeatMode(int newRepeatMode) {
        if (newRepeatMode >= 0 && newRepeatMode <= 2) {
            repeatMode = newRepeatMode;
        }
    }

    private void startProgressUpdates() {
        // Cập nhật progress bar mỗi giây
        new Thread(() -> {
            while (true) {
                try {
                    if (mediaPlayer == null || !isPlaying) {
                        break;
                    }

                    if (mediaPlayer.isPlaying()) {
                        broadcastProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
                    }

                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(TAG, "Error in progress updates: " + e.getMessage());
                    break;
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
        // Store old value for logging
        int oldProgress = currentProgress;

        // Check if seconds have changed before updating stored values
        boolean shouldUpdateNotification = (currentProgress / 1000) != (progress / 1000); // Only update when seconds
        // change

        // Update stored progress values AFTER checking
        currentProgress = progress;
        currentDuration = duration;

        Intent intent = new Intent(BROADCAST_PROGRESS);
        intent.putExtra("progress", progress);
        intent.putExtra("duration", duration);
        broadcaster.sendBroadcast(intent);

        // Update notification only when seconds change
        if (shouldUpdateNotification) {
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
        instance = null;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopLyricUpdates();
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

        // Build notification with MediaStyle
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_new)
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtistId())
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

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isShuffleEnabled() {
        return isShuffleOn;
    }

    public static boolean isServicePlaying() {
        // This should be called from a service instance, but we'll provide a static
        // method
        // You should maintain a static reference or use a different approach
        return false; // Default value, implement proper logic based on your needs
    }

    public static boolean isPlaying() {
        return instance != null && instance.mediaPlayer != null && instance.mediaPlayer.isPlaying();
    }

    public static int getCurrentPosition() {
        if (instance != null && instance.mediaPlayer != null) {
            return instance.mediaPlayer.getCurrentPosition();
        }
        return 0;
    }
}