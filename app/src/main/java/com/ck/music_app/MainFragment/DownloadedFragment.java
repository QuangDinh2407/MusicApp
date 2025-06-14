package com.ck.music_app.MainFragment;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DownloadedFragment extends Fragment {
    private RecyclerView rvLocalSongs;
    private LocalSongAdapter adapter;
    private List<LocalSong> localSongs = new ArrayList<>();
    private MediaPlayer mediaPlayer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downloaded, container, false);
        rvLocalSongs = view.findViewById(R.id.rvLocalSongs);
        rvLocalSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LocalSongAdapter(localSongs);
        rvLocalSongs.setAdapter(adapter);

        // Quét nhạc local
        loadLocalSongs();

        // Nút back
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            View fragmentContainer = requireActivity().findViewById(R.id.fragment_container);
            View viewPager = requireActivity().findViewById(R.id.view_pager);
            fragmentContainer.setVisibility(View.GONE);
            if (viewPager != null) viewPager.setVisibility(View.VISIBLE);
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        // Nút phát ngẫu nhiên
        TextView btnPlayRandom = view.findViewById(R.id.btnPlayRandom);
        btnPlayRandom.setOnClickListener(v -> playRandomSong());

        return view;
    }

    private void loadLocalSongs() {
        localSongs.clear();
        Context context = getContext();
        if (context == null) return;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        try (Cursor cursor = context.getContentResolver().query(collection, projection, selection, null, MediaStore.Audio.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    long duration = cursor.getLong(durationCol);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    String data = null;
                    try {
                        data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    } catch (Exception e) {
                        // DATA có thể bị deprecated trên Android 10+
                    }
                    Log.d("DownloadedFragment", "Found song: " + title + " - " + artist + " - " + uri.toString() + (data != null ? ("\nFile path: " + data) : ""));
                    localSongs.add(new LocalSong(title, artist, duration, uri));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void playRandomSong() {
        if (localSongs.isEmpty()) return;
        int idx = new Random().nextInt(localSongs.size());
        playSong(localSongs.get(idx));
    }

    private void playSong(LocalSong song) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getContext(), song.uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    // Model bài hát local
    static class LocalSong {
        String title, artist;
        long duration;
        Uri uri;
        LocalSong(String title, String artist, long duration, Uri uri) {
            this.title = title;
            this.artist = artist;
            this.duration = duration;
            this.uri = uri;
        }
    }

    // Adapter cho RecyclerView
    class LocalSongAdapter extends RecyclerView.Adapter<LocalSongAdapter.LocalSongViewHolder> {
        List<LocalSong> songs;
        LocalSongAdapter(List<LocalSong> songs) { this.songs = songs; }
        @NonNull
        @Override
        public LocalSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_local_song, parent, false);
            return new LocalSongViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull LocalSongViewHolder holder, int position) {
            LocalSong song = songs.get(position);
            holder.tvSongTitle.setText(song.title);
            holder.tvSongArtist.setText(song.artist);
            holder.tvSongDuration.setText(formatDuration(song.duration));
            holder.itemView.setOnClickListener(v -> playSong(song));
        }
        @Override
        public int getItemCount() { return songs.size(); }
        class LocalSongViewHolder extends RecyclerView.ViewHolder {
            TextView tvSongTitle, tvSongArtist, tvSongDuration;
            LocalSongViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
                tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
                tvSongDuration = itemView.findViewById(R.id.tvSongDuration);
            }
        }
    }

    private String formatDuration(long durationMs) {
        int totalSec = (int) (durationMs / 1000);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }
} 