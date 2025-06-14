package com.ck.music_app.Services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.ck.music_app.Model.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer;
    private List<Song> songList = new ArrayList<>();
    private int currentIndex = 0;
    private final IBinder binder = new MusicBinder();
    private boolean isPlaying = false;

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeMediaPlayer();
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(mp -> {
                if (currentIndex < songList.size() - 1) {
                    currentIndex++;
                    playNext();
                } else {
                    isPlaying = false;
                    notifyMusicUpdate();
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPlaying = false;
                notifyMusicUpdate();
                return true;
            });
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setList(List<Song> songs, int index) {
        if (songs != null) {
            this.songList = new ArrayList<>(songs);
            this.currentIndex = Math.max(0, Math.min(index, songs.size() - 1));
        }
    }

    public void playSong() {
        if (songList.isEmpty() || currentIndex < 0 || currentIndex >= songList.size()) {
            return;
        }

        try {
            if (mediaPlayer == null) {
                initializeMediaPlayer();
            }
            
            mediaPlayer.reset();
            String audioUrl = songList.get(currentIndex).getAudioUrl();
            if (audioUrl != null && !audioUrl.isEmpty()) {
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    isPlaying = true;
                    notifyMusicUpdate();
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            isPlaying = false;
            notifyMusicUpdate();
        }
    }

    public void pauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            notifyMusicUpdate();
        }
    }

    public void resumeSong() {
        if (mediaPlayer != null && !isPlaying) {
            try {
                mediaPlayer.start();
                isPlaying = true;
                notifyMusicUpdate();
            } catch (IllegalStateException e) {
                // MediaPlayer không ở trạng thái phù hợp, thử phát lại bài hát
                playSong();
            }
        }
    }

    public void playNext() {
        if (currentIndex < songList.size() - 1) {
            currentIndex++;
            playSong();
        }
    }

    public void playPrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            playSong();
        }
    }

    public boolean isPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPlaying) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(position);
            } catch (IllegalStateException e) {
                // Ignore
            }
        }
    }

    public Song getCurrentSong() {
        if (!songList.isEmpty() && currentIndex >= 0 && currentIndex < songList.size()) {
            return songList.get(currentIndex);
        }
        return null;
    }

    public List<Song> getSongList() {
        return new ArrayList<>(songList);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    private void notifyMusicUpdate() {
        // Gửi broadcast để thông báo thay đổi
        Intent intent = new Intent("MUSIC_UPDATE");
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (IllegalStateException e) {
                // Ignore
            }
            mediaPlayer = null;
        }
        super.onDestroy();
    }
} 