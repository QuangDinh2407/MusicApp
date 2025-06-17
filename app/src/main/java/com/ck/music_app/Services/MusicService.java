package com.ck.music_app.Services;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ck.music_app.Model.Song;

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

    public static final String ACTION_TOGGLE_SHUFFLE = "com.ck.music_app.action.TOGGLE_SHUFFLE";
    public static final String ACTION_TOGGLE_REPEAT = "com.ck.music_app.action.TOGGLE_REPEAT";

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
    private LocalBroadcastManager broadcaster;
    private Handler lyricHandler;
    private static final int LYRIC_UPDATE_INTERVAL = 100; // 100ms
    private static Song currentSong;

    private boolean isShuffleOn = false;
    private int repeatMode = 0; // 0: no repeat, 1: repeat all, 2: repeat one
    private List<Song> originalSongList = new ArrayList<>(); // Lưu danh sách gốc

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
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
        return START_NOT_STICKY;
    }

    private void playSong(int index) {
        if (index < 0 || index >= songList.size()) return;

        try {
            // Đảm bảo MediaPlayer được khởi tạo
            initializeMediaPlayer();
            
            // Reset và chuẩn bị MediaPlayer
            mediaPlayer.reset();
            Song song = songList.get(index);
            currentSong = song;
            currentIndex = index;
            broadcastLoadingState(true);



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
                        startProgressUpdates();
                        broadcastPlayingState(true);
                        broadcastSongChanged(currentIndex);
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
            startProgressUpdates();
            startLyricUpdates();
            broadcastPlayingState(true);
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
        if (songList.isEmpty()) return;

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
        if (songList.isEmpty()) return;

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

    private void handleShuffleMode(boolean isShuffleOn) {
        this.isShuffleOn = isShuffleOn;
        System.out.println("MusicService - Shuffle mode: " + isShuffleOn);
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
        Intent intent = new Intent(BROADCAST_PROGRESS);
        intent.putExtra("progress", progress);
        intent.putExtra("duration", duration);
        broadcaster.sendBroadcast(intent);
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
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaPlayer: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        isPlaying = false;
    }

    public static Song getCurrentSong() {
        return currentSong;
    }
} 