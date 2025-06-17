package com.ck.music_app.MainFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.Adapter.RecyclerViewAdapterSong;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.interfaces.OnSongClickListener;
import com.ck.music_app.utils.FirestoreUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TrendingFragment extends Fragment {

    private RecyclerView recyclerViewTrendingSongs;
    private RecyclerViewAdapterSong songAdapter;
    private List<Song> trendingSongsList;
    private ProgressBar progressBarTrending;
    private TextView textViewEmptyState;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trending, container, false);

        // Khởi tạo views
        recyclerViewTrendingSongs = view.findViewById(R.id.recyclerViewTrendingSongs);
        progressBarTrending = view.findViewById(R.id.progressBarTrending);
        textViewEmptyState = view.findViewById(R.id.textViewEmptyState);

        // Khởi tạo Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Khởi tạo danh sách và adapter
        trendingSongsList = new ArrayList<>();
        songAdapter = new RecyclerViewAdapterSong(getContext(), trendingSongsList);

        // Thiết lập RecyclerView
        recyclerViewTrendingSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTrendingSongs.setAdapter(songAdapter);

        // Thiết lập click listener cho adapter
        songAdapter.setOnSongClickListener((songList, position) -> {
            // Phát nhạc khi click vào bài hát
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.showPlayer(songList, position, "Thịnh hành");
            }
        });

        // Tải dữ liệu thịnh hành
        loadTrendingSongs();

        return view;
    }

    private void loadTrendingSongs() {
        showLoading(true);

        // Lấy các bài hát thịnh hành từ Firestore
        // Sắp xếp theo lượt xem (viewCount) giảm dần, giới hạn 50 bài
        db.collection("songs")
                .orderBy("viewCount", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    trendingSongsList.clear();
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Song song = document.toObject(Song.class);
                            song.setSongId(document.getId());
                            trendingSongsList.add(song);
                        }
                        songAdapter.notifyDataSetChanged();
                        showEmptyState(false);
                    }
                    
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showEmptyState(true);
                    
                    // Fallback: Nếu không có trường viewCount, lấy bài hát mới nhất
                    loadLatestSongs();
                });
    }

    private void loadLatestSongs() {
        // Fallback: Lấy các bài hát mới nhất nếu không có dữ liệu viewCount
        db.collection("songs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    trendingSongsList.clear();
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Song song = document.toObject(Song.class);
                            song.setSongId(document.getId());
                            trendingSongsList.add(song);
                        }
                        songAdapter.notifyDataSetChanged();
                        showEmptyState(false);
                    }
                })
                .addOnFailureListener(e -> {
                    // Nếu vẫn lỗi, lấy tất cả bài hát
                    loadAllSongs();
                });
    }

    private void loadAllSongs() {
        // Fallback cuối cùng: Lấy tất cả bài hát
        FirestoreUtils.getAllSongs(new FirestoreUtils.FirestoreCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> songs) {
                trendingSongsList.clear();
                // Lấy tối đa 30 bài đầu tiên
                int limit = Math.min(songs.size(), 30);
                for (int i = 0; i < limit; i++) {
                    trendingSongsList.add(songs.get(i));
                }
                songAdapter.notifyDataSetChanged();
                showEmptyState(trendingSongsList.isEmpty());
            }

            @Override
            public void onError(Exception e) {
                showEmptyState(true);
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBarTrending != null) {
            progressBarTrending.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewTrendingSongs != null) {
            recyclerViewTrendingSongs.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmptyState(boolean show) {
        if (textViewEmptyState != null) {
            textViewEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewTrendingSongs != null) {
            recyclerViewTrendingSongs.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh dữ liệu khi fragment được hiển thị lại
        if (trendingSongsList != null && trendingSongsList.isEmpty()) {
            loadTrendingSongs();
        }
    }
} 