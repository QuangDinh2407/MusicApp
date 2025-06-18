package com.ck.music_app.MainFragment.HomeChildFragment;

import android.content.Intent;
import android.graphics.Color;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.ck.music_app.interfaces.OnSongClickListener;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.AppBarLayout;

import com.bumptech.glide.Glide;
import com.ck.music_app.Adapter.RecyclerViewAdapterSong;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.utils.FavoriteAlbumUtils;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

public class AlbumSongsFragment extends Fragment implements OnSongClickListener {
    private static final String BROADCAST_FAVORITE_UPDATED = "favorite_album_updated";
    private List<Song> songs;
    private String albumName;
    private String coverUrl;
    private String artistName;
    private ImageView imgAlbumCover, imgBackground;
    private ImageButton btnBack, btnFavorite;
    private boolean isFavorite = false;
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private TextView tvAlbumTitle;
    private TextView tvArtistName;
    private RecyclerView recyclerView;
    private RecyclerViewAdapterSong adapter;

    private OnFragmentDismissListener dismissListener;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

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

    public static AlbumSongsFragment newInstance(List<Song> songs, String albumName, String coverUrl, String artistName) {
        AlbumSongsFragment fragment = new AlbumSongsFragment();
        Bundle args = new Bundle();
        args.putSerializable("songs", new ArrayList<>(songs));
        args.putString("albumName", albumName);
        args.putString("coverUrl", coverUrl);
        args.putString("artistName", artistName);
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
            artistName = getArguments().getString("artistName");

        }
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
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
        btnFavorite = view.findViewById(R.id.btnFavorite);

        // Set up heart button click listener
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        // Set up collapsing toolbar
        collapsingToolbar.setTitle("");  // Empty title initially
        tvAlbumTitle.setText(albumName);

        // Hiển thị ảnh album
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                .load(coverUrl)
                .placeholder(R.drawable.default_album_art)
                .error(R.drawable.default_album_art)
                .into(imgAlbumCover);

            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .into(imgBackground);
        } else {
            // Nếu không có ảnh bìa, hiển thị ảnh mặc định
            imgAlbumCover.setImageResource(R.drawable.default_album_art);
            imgBackground.setImageResource(R.drawable.default_album_art);
        }

        if (songs != null && !songs.isEmpty()) {
            // Có bài hát, hiển thị danh sách
            adapter = new RecyclerViewAdapterSong(requireContext(), songs);
            adapter.setOnSongClickListener(this);
            recyclerView.setAdapter(adapter);
            tvArtistName.setText(artistName); // hoặc lấy tên artist từ bài hát đầu tiên
            recyclerView.setVisibility(View.VISIBLE);
            view.findViewById(R.id.emptyStateLayout).setVisibility(View.GONE);
        } else {
            // Không có bài hát, hiển thị empty state
            recyclerView.setVisibility(View.GONE);
            view.findViewById(R.id.emptyStateLayout).setVisibility(View.VISIBLE);
        }

        // Handle collapsing state
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }

                // Tính toán phần trăm scroll
                float percentage = (float) Math.abs(verticalOffset) / scrollRange;
                
                if (scrollRange + verticalOffset == 0) {
                    // Collapsed
                    collapsingToolbar.setTitle(albumName);
                    collapsingToolbar.setCollapsedTitleTextColor(getResources().getColor(R.color.textPrimary));
                    btnBack.setColorFilter(getResources().getColor(R.color.textPrimary));
                    isShow = true;
                } else if (isShow) {
                    // Expanded
                    collapsingToolbar.setTitle("");
                    btnBack.setColorFilter(getResources().getColor(R.color.white));
                    isShow = false;
                }

                // Tạo animation màu mượt cho icon back
                int colorFrom = getResources().getColor(R.color.white);
                int colorTo = getResources().getColor(R.color.textPrimary);
                
                // Blend màu dựa trên phần trăm scroll
                int blendedColor = blendColors(colorFrom, colorTo, percentage);
                btnBack.setColorFilter(blendedColor);
            }
        });

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                    .remove(this)
                    .commit();
                getParentFragmentManager().popBackStack();
            }
        });

        // Check if album is already favorited
        if (currentUser != null) {
            FavoriteAlbumUtils.checkFavoriteStatus(currentUser.getUid(), albumName, new FavoriteAlbumUtils.FavoriteCallback() {
                @Override
                public void onSuccess(boolean isFavorited) {
                    isFavorite = isFavorited;
                    updateFavoriteButton();
                }

                @Override
                public void onError() {
                    // Handle error if needed
                }
            });
        }

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
            ((MainActivity) getActivity()).showPlayer(songList, position, albumName);
        }
    }

    private void toggleFavorite() {
        FavoriteAlbumUtils.toggleFavoriteAlbum(
            requireContext(),
            albumName,
            coverUrl,
            isFavorite,
            new FavoriteAlbumUtils.FavoriteCallback() {
                @Override
                public void onSuccess(boolean newFavoriteState) {
                    isFavorite = newFavoriteState;
                    updateFavoriteButton();
                }

                @Override
                public void onError() {
                    // Revert UI on error
                    isFavorite = !isFavorite;
                    updateFavoriteButton();
                }
            }
        );
    }

    private void updateFavoriteButton() {
        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_heart_full);
            btnFavorite.setColorFilter(getResources().getColor(R.color.accent_orange));
        } else {
            btnFavorite.setImageResource(R.drawable.ic_heart);
            btnFavorite.setColorFilter(getResources().getColor(android.R.color.white));
        }
    // Thêm helper method để blend màu
    private int blendColors(int from, int to, float ratio) {
        float inverseRatio = 1f - ratio;
        
        float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
        float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
        float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;
        
        return Color.rgb((int) r, (int) g, (int) b);
    }
}   