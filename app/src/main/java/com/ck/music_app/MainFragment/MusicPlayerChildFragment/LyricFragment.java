package com.ck.music_app.MainFragment.MusicPlayerChildFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ck.music_app.MainFragment.MusicPlayerFragment;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.utils.GradientUtils;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LyricFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LyricFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View gradientOverlay;
    private TextView tvLyrics;
    private List<Song> songList;
    private int currentIndex;
    private LocalBroadcastManager broadcastManager;

    private final BroadcastReceiver songUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case "UPDATE_SONG_INFO":
                        String coverUrl = intent.getStringExtra("COVER_URL");
                        String lyric = intent.getStringExtra("LYRIC");
                        updateUI(coverUrl,lyric);
                        break;
                }
            }
        }
    };

    public LyricFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LyricFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LyricFragment newInstance(String param1, String param2) {
        LyricFragment fragment = new LyricFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        
        // Lấy thông tin bài hát từ parent fragment
        MusicPlayerFragment parentFragment = (MusicPlayerFragment) getParentFragment();
        if (parentFragment != null) {
            songList = parentFragment.getSongList();
            currentIndex = parentFragment.getCurrentIndex();
        }
        
        broadcastManager = LocalBroadcastManager.getInstance(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lyric, container, false);
        gradientOverlay = view.findViewById(R.id.gradientOverlay);
        tvLyrics = view.findViewById(R.id.tvLyrics);
        
        // Update initial background
        if (songList != null && currentIndex >= 0 && currentIndex < songList.size()) {
            updateUI(songList.get(currentIndex).getCoverUrl(),songList.get(currentIndex).getLyrics());
        }
        
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_SONG_INFO");
        broadcastManager.registerReceiver(songUpdateReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        broadcastManager.unregisterReceiver(songUpdateReceiver);
    }

    private void updateUI(String coverUrl, String lyric) {
        if (coverUrl != null) {
            Glide.with(this)
                    .asBitmap()
                    .load(coverUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            if (isAdded()) {
                                GradientUtils.createGradientFromBitmap(resource, gradientOverlay);
                            }
                        }

                        @Override
                        public void onLoadCleared(Drawable placeholder) {}
                    });
        }

        tvLyrics.setText(lyric);
    }
}