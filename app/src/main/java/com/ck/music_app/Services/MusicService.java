package com.ck.music_app.Services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.ck.music_app.Model.Song;
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

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
        mediaPlayer = new MediaPlayer();
        lyricHandler = new Handler(Looper.getMainLooper());
        
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
            }
        }
        return START_NOT_STICKY;
    }

    private void playSong(int index) {
        if (index < 0 || index >= songList.size()) return;

        try {
            mediaPlayer.reset();
            Song song = songList.get(index);
            broadcastLoadingState(true);
            mediaPlayer.setDataSource(song.getAudioUrl());
            mediaPlayer.prepareAsync();
            
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                isPlaying = true;
                startProgressUpdates();
                broadcastPlayingState(true);
                broadcastSongChanged(currentIndex);
                broadcastLoadingState(false);
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

    private void playPrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            playSong(currentIndex);
        }
    }

    private void playNext() {
        if (currentIndex < songList.size() - 1) {
            currentIndex++;
            playSong(currentIndex);
        }
    }

    private void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
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
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
} 