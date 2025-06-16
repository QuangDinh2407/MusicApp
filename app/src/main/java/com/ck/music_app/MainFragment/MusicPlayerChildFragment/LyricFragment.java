package com.ck.music_app.MainFragment.MusicPlayerChildFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ck.music_app.Adapter.LyricAdapter;
import com.ck.music_app.MainFragment.MusicPlayerFragment;
import com.ck.music_app.Model.LyricLine;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.utils.GradientUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private RecyclerView rvLyrics;
    private TextView tvNoLyrics;
    private LyricAdapter lyricAdapter;
    private List<Song> songList;
    private int currentIndex;
    private LocalBroadcastManager broadcastManager;
    private static final String TAG = "LyricFragment";

    private final BroadcastReceiver songUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case "UPDATE_SONG_INFO":
                        String lyric = intent.getStringExtra("LYRIC");
                        updateUI(lyric);
                        break;
                    case MusicService.BROADCAST_LYRIC_POSITION:
                        int position = intent.getIntExtra("position", 0);
                        updateLyricHighlight(position);
                        break;
                    case MusicService.BROADCAST_PROGRESS:
                        int progress = intent.getIntExtra("progress", 0);
                        updateLyricHighlight(progress);
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
        rvLyrics = view.findViewById(R.id.rvLyrics);
        tvNoLyrics = view.findViewById(R.id.tvNoLyrics);
        
        setupRecyclerView();
        
        if (songList != null && currentIndex >= 0 && currentIndex < songList.size()) {
            updateUI(songList.get(currentIndex).getLyrics());
        }
        
        return view;
    }

    private void setupRecyclerView() {
        lyricAdapter = new LyricAdapter(requireContext());
        rvLyrics.setAdapter(lyricAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvLyrics.setLayoutManager(layoutManager);
        rvLyrics.setClipToPadding(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_SONG_INFO");
        filter.addAction(MusicService.BROADCAST_LYRIC_POSITION);
        filter.addAction(MusicService.BROADCAST_PROGRESS);
        broadcastManager.registerReceiver(songUpdateReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        broadcastManager.unregisterReceiver(songUpdateReceiver);
    }

    private void updateUI(String lyric) {

        if (lyric != null) {
            List<LyricLine> lyricLines = parseLyrics(lyric);
            lyricAdapter.setLyrics(lyricLines);
        }
    }

    private class CustomSmoothScroller extends LinearSmoothScroller {
        private static final float MILLISECONDS_PER_INCH = 100f; // Điều chỉnh tốc độ scroll

        public CustomSmoothScroller(Context context) {
            super(context);
        }

        @Override
        protected int getVerticalSnapPreference() {
            return SNAP_TO_START; // Snap view to the top
        }

        @Override
        protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
            return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
        }
    }

    private void updateLyricHighlight(int position) {
        // Log thời gian nhận được từ service
        lyricAdapter.updateHighlight(position);
        
        // Tìm dòng đang được highlight
        int highlightedPosition = -1;
        for (int i = 0; i < lyricAdapter.getItemCount(); i++) {
            if (lyricAdapter.isHighlighted(i)) {
                highlightedPosition = i;
                break;
            }
        }
        
        if (highlightedPosition != -1) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) rvLyrics.getLayoutManager();
            if (layoutManager != null) {
                // Tạo CustomSmoothScroller
                CustomSmoothScroller smoothScroller = new CustomSmoothScroller(requireContext());
                smoothScroller.setTargetPosition(highlightedPosition);
                layoutManager.startSmoothScroll(smoothScroller);
            }
        }
    }

    private boolean isItemFullyVisible(LinearLayoutManager layoutManager, int position) {
        int first = layoutManager.findFirstCompletelyVisibleItemPosition();
        int last = layoutManager.findLastCompletelyVisibleItemPosition();
        return position >= first && position <= last;
    }

    private List<LyricLine> parseLyrics(String lyrics) {
        List<LyricLine> lyricLines = new ArrayList<>();
        
        // Kiểm tra nếu lyrics null hoặc rỗng
        if (lyrics == null || lyrics.trim().isEmpty()) {
            showNoLyrics(true);
            return lyricLines;
        }

        String[] lines = lyrics.split("\n");
        Pattern timePattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{1,3})\\](.*)");
        
        boolean hasValidLyrics = false;
        for (String line : lines) {
            Matcher matcher = timePattern.matcher(line);
            if (matcher.find()) {
                hasValidLyrics = true;
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                int milliseconds = Integer.parseInt(matcher.group(3));
                String text = matcher.group(4).trim();

                long timeMs = (minutes * 60 * 1000L) + (seconds * 1000L) + milliseconds;
                lyricLines.add(new LyricLine(timeMs, text));
            }
        }

        // Nếu không tìm thấy lyrics hợp lệ
        if (!hasValidLyrics) {
            showNoLyrics(true);
        } else {
            showNoLyrics(false);
        }
        
        return lyricLines;
    }

    private void showNoLyrics(boolean show) {
        if (show) {
            tvNoLyrics.setVisibility(View.VISIBLE);
            rvLyrics.setVisibility(View.GONE);
        } else {
            tvNoLyrics.setVisibility(View.GONE);
            rvLyrics.setVisibility(View.VISIBLE);
        }
    }
}