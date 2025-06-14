package com.ck.music_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.ck.music_app.Adapter.AlbumSongsAdapter;
import com.ck.music_app.Model.Song;
import com.ck.music_app.Services.FirebaseService;

import org.checkerframework.common.returnsreceiver.qual.This;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AlbumSongsActivity extends AppCompatActivity {
    private ImageView imgCover;
    private TextView tvAlbumTitle;
    private TextView tvArtistName;
    private ListView listViewSongs;
    private List<Song> songList = new ArrayList<>();
    private AlbumSongsAdapter adapter;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_songs);

        imgCover = findViewById(R.id.imgCover);
        tvAlbumTitle = findViewById(R.id.tvAlbumTitle);
        tvArtistName = findViewById(R.id.tvArtistName);
        listViewSongs = findViewById(R.id.listViewSongs);

        String albumId = getIntent().getStringExtra("albumId");
        String albumName = getIntent().getStringExtra("albumName");
        String albumImage = getIntent().getStringExtra("albumImage");
        String artistName = getIntent().getStringExtra("artistName");

        tvAlbumTitle.setText(albumName);
        tvArtistName.setText(artistName);

        Glide.with(this)
                .load(albumImage)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imgCover);

        adapter = new AlbumSongsAdapter(this, songList);
        listViewSongs.setAdapter(adapter);

        firebaseService = FirebaseService.getInstance();
        loadSongs(albumId);

        listViewSongs.setOnItemClickListener((parent, v, position, id) -> {
            Song selectedSong = songList.get(position);
            String songId = selectedSong.getSongId();
            
            // Debug log
            Log.d("AlbumSongsActivity", "Clicked song: " + selectedSong.getTitle());
            Log.d("AlbumSongsActivity", "Song ID: " + songId);
            Log.d("AlbumSongsActivity", "Position: " + position);
            
            // Lưu ID bài hát vào Firebase
            firebaseService.updateSongPlaying(songId, new FirebaseService.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d("AlbumSongsActivity", "Successfully saved songId to Firebase: " + songId);
                    // Sau khi lưu thành công, chuyển sang PlayMusicActivity
                    Intent intent = new Intent(AlbumSongsActivity.this, PlayMusicActivity.class);
                    intent.putExtra("songList", (Serializable) songList);
                    intent.putExtra("currentIndex", position);
                    startActivity(intent);
                }

                @Override
                public void onError(Exception e) {
                    Log.e("AlbumSongsActivity", "Error saving songId to Firebase", e);
                    // Nếu lỗi vẫn cho phép phát nhạc
                    Toast.makeText(AlbumSongsActivity.this, 
                        "Không thể lưu bài hát: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(AlbumSongsActivity.this, PlayMusicActivity.class);
                    intent.putExtra("songList", (Serializable) songList);
                    intent.putExtra("currentIndex", position);
                    startActivity(intent);
                }
            });
        });
    }

    private void loadSongs(String albumId) {
        firebaseService.getSongsByAlbumId(albumId, new FirebaseService.FirestoreCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> songs) {
                songList.clear();
                songList.addAll(songs);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AlbumSongsActivity.this, 
                    "Lỗi khi tải danh sách bài hát: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
} 