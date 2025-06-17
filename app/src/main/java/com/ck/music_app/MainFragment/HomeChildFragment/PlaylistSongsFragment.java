package com.ck.music_app.MainFragment.HomeChildFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.FirebaseService;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.utils.FirestoreUtils;

import java.util.ArrayList;
import java.util.List;

public class PlaylistSongsFragment extends Fragment implements OnSongClickListener {
    private Playlist playlist;
    private List<Song> songs = new ArrayList<>();
    private ImageView imgPlaylistCover, imgBackground;
    private ImageButton btnBack;
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private TextView tvPlaylistTitle, tvPlaylistCreator, tvSongCount;
    private RecyclerView recyclerView;
    private RecyclerViewAdapterSong adapter;
    private FirebaseService firebaseService;

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

    public static PlaylistSongsFragment newInstance(Playlist playlist) {
        PlaylistSongsFragment fragment = new PlaylistSongsFragment();
        Bundle args = new Bundle();
        args.putSerializable("playlist", playlist);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlist = (Playlist) getArguments().getSerializable("playlist");
        }
        firebaseService = FirebaseService.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_songs, container, false);

        // Initialize views
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar);
        appBarLayout = view.findViewById(R.id.appBarLayout);
        tvPlaylistTitle = view.findViewById(R.id.tvPlaylistTitle);
        tvPlaylistCreator = view.findViewById(R.id.tvPlaylistCreator);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        recyclerView = view.findViewById(R.id.listViewSongs);
        imgPlaylistCover = view.findViewById(R.id.imgCover);
        imgBackground = view.findViewById(R.id.imgBackground);
        btnBack = view.findViewById(R.id.btnBack);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        // Set up collapsing toolbar
        setupCollapsingToolbar();

        // Load playlist info
        if (playlist != null) {
            setupPlaylistInfo();
            loadPlaylistSongs();
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

    private void setupCollapsingToolbar() {
        collapsingToolbar.setTitle(""); // Empty title initially

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
                    collapsingToolbar.setTitle(playlist != null ? playlist.getName() : "Playlist");
                    isShow = true;
                } else if (isShow) {
                    // Expanded
                    collapsingToolbar.setTitle("");
                    isShow = false;
                }
            }
        });
    }

    private void setupPlaylistInfo() {
        tvPlaylistTitle.setText(playlist.getName());
        tvPlaylistCreator.setText(playlist.getUserId() != null ? playlist.getUserId() : "Unknown");

        // Set song count
        if (playlist.getSongIds() != null) {
            tvSongCount.setText(playlist.getSongIds().size() + " bài hát");
        } else {
            tvSongCount.setText("0 bài hát");
        }

        // Load playlist cover
        String coverUrl = playlist.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.playlist_default)
                    .error(R.drawable.playlist_default)
                    .into(imgPlaylistCover);

            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.playlist_default)
                    .error(R.drawable.playlist_default)
                    .into(imgBackground);
        } else {
            imgPlaylistCover.setImageResource(R.drawable.playlist_default);
            imgBackground.setImageResource(R.drawable.playlist_default);
        }
    }

    private void loadPlaylistSongs() {
        if (playlist.getSongIds() == null || playlist.getSongIds().isEmpty()) {
            Toast.makeText(getContext(), "Playlist trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use FirestoreUtils to get songs by playlist ID
        FirestoreUtils.getSongsByPlaylistId(playlist.getId(), new FirestoreUtils.FirestoreCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> songList) {
                songs.clear();
                songs.addAll(songList);

                adapter = new RecyclerViewAdapterSong(requireContext(), songs);
                adapter.setOnSongClickListener(PlaylistSongsFragment.this);
                recyclerView.setAdapter(adapter);

                // Update song count
                tvSongCount.setText(songs.size() + " bài hát");
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Lỗi khi tải danh sách bài hát", Toast.LENGTH_SHORT).show();
            }
        });
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
            ((MainActivity) getActivity()).showPlayer(songList, position, playlist.getName());
        }
    }
}