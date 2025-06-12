package com.ck.music_app.MainFragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.PlayMusicActivity;
import com.ck.music_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private ListView listViewSongs;
    private ArrayList<Song> songList = new ArrayList<>();
    private SongAdapter songAdapter;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (getArguments() != null) {
                mParam1 = getArguments().getString(ARG_PARAM1);
                mParam2 = getArguments().getString(ARG_PARAM2);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_home, container, false);

            listViewSongs = view.findViewById(R.id.listViewSongs);
            songAdapter = new SongAdapter();
            listViewSongs.setAdapter(songAdapter);
            
            Log.d(TAG, "Starting to load songs");
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            db.collection("songs")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        try {
//                            if (task.isSuccessful() && task.getResult() != null) {
//                                songList.clear();
//                                for (QueryDocumentSnapshot document : task.getResult()) {
//                                    try {
//                                        Song song = new Song();
//                                        // Kiểm tra và xử lý từng trường dữ liệu
//                                        song.setSongId(document.getId());
//
//                                        // Xử lý các trường String
//                                        String albumId = document.getString("albumId");
//                                        String artistId = document.getString("artistId");
//                                        String audioUrl = document.getString("audioUrl");
//                                        String coverUrl = document.getString("coverUrl");
//                                        String createdAt = document.getString("createdAt");
//                                        String genreId = document.getString("genreId");
//                                        String title = document.getString("title");
//
//                                        // Xử lý các trường Long
//                                        Long duration = document.getLong("duration");
//                                        Long likeCount = document.getLong("likeCount");
//                                        Long viewCount = document.getLong("viewCount");
//
//                                        // Gán giá trị cho song object
//                                        song.setAlbumId(albumId != null ? albumId : "");
//                                        song.setArtistId(artistId != null ? artistId : "");
//                                        song.setAudioUrl(audioUrl != null ? audioUrl : "");
//                                        song.setCoverUrl(coverUrl != null ? coverUrl : "");
//                                        song.setCreateAt(createdAt != null ? createdAt : "");
//                                        song.setDuration(duration != null ? duration.intValue() : 0);
//                                        song.setGenreId(genreId != null ? genreId : "");
//                                        song.setLikeCount(likeCount != null ? likeCount.intValue() : 0);
//                                        song.setTitle(title != null ? title : "");
//                                        song.setViewCount(viewCount != null ? viewCount.intValue() : 0);
//
//                                        songList.add(song);
//                                        Log.d(TAG, "Added song: " + song.getTitle());
//                                    } catch (Exception e) {
//                                        Log.e(TAG, "Error processing song document: " + e.getMessage(), e);
//                                        // Tiếp tục với bài hát tiếp theo nếu có lỗi
//                                        continue;
//                                    }
//                                }
//
//                                if (songAdapter != null) {
//                                    songAdapter.notifyDataSetChanged();
//                                    Log.d(TAG, "Songs loaded successfully. Count: " + songList.size());
//                                } else {
//                                    Log.e(TAG, "SongAdapter is null");
//                                }
//                            } else {
//                                String errorMessage = task.getException() != null ?
//                                    task.getException().getMessage() : "Unknown error";
//                                Log.e(TAG, "Error loading songs: " + errorMessage);
//                                if (getContext() != null) {
//                                    Toast.makeText(getContext(),
//                                        "Lỗi khi tải danh sách bài hát: " + errorMessage,
//                                        Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        } catch (Exception e) {
//                            Log.e(TAG, "Error in onComplete: " + e.getMessage(), e);
//                            if (getContext() != null) {
//                                Toast.makeText(getContext(),
//                                    "Lỗi khi xử lý dữ liệu bài hát: " + e.getMessage(),
//                                    Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }
//                });

            listViewSongs.setOnItemClickListener((parent, v, position, id) -> {
                try {
                    Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
                    intent.putExtra("songList", (Serializable) songList);
                    intent.putExtra("currentIndex", position);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting PlayMusicActivity: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Lỗi khi mở bài hát", Toast.LENGTH_SHORT).show();
                }
            });

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return inflater.inflate(R.layout.fragment_home, container, false);
        }
    }

    class SongAdapter extends ArrayAdapter<Song> {
        public SongAdapter() {
            super(getActivity(), 0, songList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_song, parent, false);
                }
                Song song = getItem(position);
                if (song == null) return convertView;

                ImageView imgCover = convertView.findViewById(R.id.imgCover);
                TextView tvTitle = convertView.findViewById(R.id.tvTitle);
                TextView tvViewCount = convertView.findViewById(R.id.tvViewCount);

                tvTitle.setText(song.getTitle());
                tvViewCount.setText("Lượt nghe: " + song.getViewCount());

                if (getContext() != null) {
                    Glide.with(requireContext())
                            .load(song.getCoverUrl())
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .into(imgCover);
                }

                return convertView;
            } catch (Exception e) {
                Log.e(TAG, "Error in getView: " + e.getMessage(), e);
                return convertView;
            }
        }
    }
}