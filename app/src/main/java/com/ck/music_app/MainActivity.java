package com.ck.music_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import java.io.Serializable;
import java.util.ArrayList;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.Song;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;



public class MainActivity extends AppCompatActivity {
    private ListView listViewSongs;
    private ArrayList<Song> songList = new ArrayList<>();
    private SongAdapter songAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        TextView helloText = findViewById(R.id.helloText);
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            helloText.setText("Hello " + email);
        }

        listViewSongs = findViewById(R.id.listViewSongs);
        songAdapter = new SongAdapter();
        listViewSongs.setAdapter(songAdapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("songs").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    songList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Song song = new Song();
                        song.setSongId(document.getId());
                        song.setAlbumId(document.getString("albumId"));
                        song.setArtistId(document.getString("artistId"));
                        song.setAudioUrl(document.getString("audioUrl"));
                        song.setCoverUrl(document.getString("coverUrl"));
                        song.setCreateAt(document.getString("createdAt"));
                        song.setDuration(document.getLong("duration") != null ? document.getLong("duration").intValue() : 0);
                        song.setGenreId(document.getString("genreId"));
                        song.setLikeCount(document.getLong("likeCount") != null ? document.getLong("likeCount").intValue() : 0);
                        song.setTitle(document.getString("title"));
                        song.setViewCount(document.getLong("viewCount") != null ? document.getLong("viewCount").intValue() : 0);
                        songList.add(song);
                    }
                    songAdapter.notifyDataSetChanged();
                }
            }
        });

        

        listViewSongs.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainActivity.this, PlayMusicActivity.class);
            intent.putExtra("songList", (Serializable) songList);
            intent.putExtra("currentIndex", position);
            startActivity(intent);
        
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    class SongAdapter extends ArrayAdapter<Song> {
        public SongAdapter() {
            super(MainActivity.this, 0, songList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_song, parent, false);
            }
            Song song = getItem(position);

            ImageView imgCover = convertView.findViewById(R.id.imgCover);
            TextView tvTitle = convertView.findViewById(R.id.tvTitle);
            TextView tvViewCount = convertView.findViewById(R.id.tvViewCount);

            tvTitle.setText(song.getTitle());
            tvViewCount.setText("Lượt nghe: " + song.getViewCount());


            Glide.with(convertView.getContext())
                    .load(song.getCoverUrl())
                    .placeholder(R.mipmap.ic_launcher) // Ảnh mặc định khi loading
                    .error(R.mipmap.ic_launcher)       // Ảnh khi lỗi
                    .into(imgCover);

            return convertView;
        }
    }
}