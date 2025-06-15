package com.ck.music_app.MainFragment.HomeChildFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.ck.music_app.Adapter.SongAdapter;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;

import java.util.ArrayList;
import java.util.List;

public class AlbumSongsFragment extends Fragment {
    private List<Song> songs;
    private String albumName;
    private String coverUrl;
    private ImageView imgAlbumCover, imgBackground;

    private ImageButton btnBack;

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

        TextView tvAlbumName = view.findViewById(R.id.tvAlbumTitle);
        ListView listView = view.findViewById(R.id.listViewSongs);
        imgAlbumCover = view.findViewById(R.id.imgCover);
        imgBackground = view.findViewById(R.id.imgBackground);
        btnBack = view.findViewById(R.id.btnBack);
        tvAlbumName.setText(albumName);

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
            SongAdapter adapter = new SongAdapter(requireContext(), songs);
            adapter.setOnSongClickListener((songList, position) -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showPlayer(songList, position);
                }
            });
            listView.setAdapter(adapter);
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
} 