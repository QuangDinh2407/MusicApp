package com.ck.music_app.MainFragment.HomeChildFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.ck.music_app.Adapter.AlbumSongsAdapter;
import com.ck.music_app.Model.Song;
import com.ck.music_app.PlayMusicActivity;
import com.ck.music_app.R;
import com.ck.music_app.Services.FirebaseService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AlbumSongsFragment extends Fragment {
    private ImageView imgCover;
    private ImageView imgBackground;
    private TextView tvAlbumTitle;
    private TextView tvArtistName;
    private ListView listViewSongs;
    private ImageButton btnBack;
    private List<Song> songList = new ArrayList<>();
    private AlbumSongsAdapter adapter;
    private FirebaseService firebaseService;
    
    private String albumId;
    private String albumName;
    private String albumImage;
    private String artistName;

    public static AlbumSongsFragment newInstance(String albumId, String albumName, String albumImage, String artistName) {
        AlbumSongsFragment fragment = new AlbumSongsFragment();
        Bundle args = new Bundle();
        args.putString("albumId", albumId);
        args.putString("albumName", albumName);
        args.putString("albumImage", albumImage);
        args.putString("artistName", artistName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            albumId = getArguments().getString("albumId");
            albumName = getArguments().getString("albumName");
            albumImage = getArguments().getString("albumImage");
            artistName = getArguments().getString("artistName");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_songs, container, false);

        imgCover = view.findViewById(R.id.imgCover);
        imgBackground = view.findViewById(R.id.imgBackground);
        tvAlbumTitle = view.findViewById(R.id.tvAlbumTitle);
        tvArtistName = view.findViewById(R.id.tvArtistName);
        listViewSongs = view.findViewById(R.id.listViewSongs);
        btnBack = view.findViewById(R.id.btnBack);
        
        tvAlbumTitle.setText(albumName);
        tvArtistName.setText(artistName);

        // Load ảnh album
        Glide.with(requireContext())
                .load(albumImage)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imgCover);

        // Load ảnh nền
        Glide.with(requireContext())
                .load(albumImage)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imgBackground);

        adapter = new AlbumSongsAdapter(requireContext(), songList);
        listViewSongs.setAdapter(adapter);

        firebaseService = FirebaseService.getInstance();
        loadSongs(albumId);

        listViewSongs.setOnItemClickListener((parent, v, position, id) -> {
            Intent intent = new Intent(requireContext(), PlayMusicActivity.class);
            intent.putExtra("songList", (Serializable) songList);
            intent.putExtra("currentIndex", position);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
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
                Toast.makeText(requireContext(), 
                    "Lỗi khi tải danh sách bài hát: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
} 