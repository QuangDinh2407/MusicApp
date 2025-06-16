package com.ck.music_app.MainFragment.HomeChildFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.interfaces.OnSongClickListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.AppBarLayout;

import com.bumptech.glide.Glide;
import com.ck.music_app.Adapter.RecyclerViewAdapterSong;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.MusicService;

import java.util.ArrayList;
import java.util.List;

public class AlbumSongsFragment extends Fragment implements OnSongClickListener {
    private List<Song> songs;
    private String albumName;
    private String coverUrl;
    private ImageView imgAlbumCover, imgBackground;
    private ImageButton btnBack;
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private TextView tvAlbumTitle;
    private TextView tvArtistName;
    private RecyclerView recyclerView;
    private RecyclerViewAdapterSong adapter;

    private OnFragmentDismissListener dismissListener;

    public interface OnFragmentDismissListener {
        void onDismiss();
    }

    public void setOnFragmentDismissListener(OnFragmentDismissListener listener) {
        this.dismissListener = listener;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }

    public static AlbumSongsFragment newInstance(List<Song> songs, String albumName, String coverUrl) {
        AlbumSongsFragment fragment = new AlbumSongsFragment();
        Bundle args = new Bundle();
        args.putSerializable("songs", new ArrayList<>(songs));
        args.putString("albumName", albumName);
        args.putString("coverUrl", coverUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songs = (List<Song>) getArguments().getSerializable("songs");
            albumName = getArguments().getString("albumName");
            coverUrl = getArguments().getString("coverUrl");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_songs, container, false);

        // Initialize views
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar);
        appBarLayout = view.findViewById(R.id.appBarLayout);
        tvAlbumTitle = view.findViewById(R.id.tvAlbumTitle);
        tvArtistName = view.findViewById(R.id.tvArtistName);
        recyclerView = view.findViewById(R.id.listViewSongs);
        imgAlbumCover = view.findViewById(R.id.imgCover);
        imgBackground = view.findViewById(R.id.imgBackground);
        btnBack = view.findViewById(R.id.btnBack);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        // Set up collapsing toolbar
        collapsingToolbar.setTitle("");  // Empty title initially
        tvAlbumTitle.setText(albumName);
        tvArtistName.setText("Various Artists"); // You can set this dynamically

        // Handle collapsing state
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    // Collapsed
                    collapsingToolbar.setTitle(albumName);
                    isShow = true;
                } else if (isShow) {
                    // Expanded
                    collapsingToolbar.setTitle("");
                    isShow = false;
                }
            }
        });

        // Hiển thị ảnh album
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                .load(coverUrl)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imgAlbumCover);

            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(imgBackground);
        }

        if (songs != null) {
            adapter = new RecyclerViewAdapterSong(requireContext(), songs);
            adapter.setOnSongClickListener(this);  // Sử dụng OnSongClickListener
            recyclerView.setAdapter(adapter);
        }

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                    .remove(this)
                    .commit();
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
    }

    @Override
    public void onSongClick(List<Song> songList, int position) {
        // Gửi intent đến Service để phát nhạc
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra("songList", new ArrayList<>(songList));
        intent.putExtra("position", position);
        requireContext().startService(intent);

        // Hiển thị player fragment
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showPlayer(songList, position);
        }
    }
} 