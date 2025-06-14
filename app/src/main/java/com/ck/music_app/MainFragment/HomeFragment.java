package com.ck.music_app.MainFragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

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
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        listViewSongs = view.findViewById(R.id.listViewSongs);
        songAdapter = new SongAdapter();
        listViewSongs.setAdapter(songAdapter);

        // Load songs in background with timeout
        loadSongsAsync();

        listViewSongs.setOnItemClickListener((parent, v, position, id) -> {
            Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
            intent.putExtra("songList", (Serializable) songList);
            intent.putExtra("currentIndex", position);
            startActivity(intent);
        });

        return view;
    }

    private void loadSongsAsync() {
        // Load songs in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // Add timeout to prevent hanging
                db.collection("songs")
                        .limit(50) // Limit initial load to 50 songs for better performance
                        .get()
                        .addOnCompleteListener(task -> {
                            if (getActivity() == null)
                                return; // Fragment might be destroyed

                            getActivity().runOnUiThread(() -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    songList.clear();
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        try {
                                            Song song = new Song();
                                            song.setSongId(document.getId());

                                            // Handle albumId safely
                                            Object albumIdObj = document.get("albumId");
                                            if (albumIdObj instanceof List) {
                                                List<String> albumIds = (List<String>) albumIdObj;
                                                if (!albumIds.isEmpty()) {
                                                    song.setAlbumId(albumIds.get(0));
                                                }
                                            } else if (albumIdObj instanceof String) {
                                                song.setAlbumId((String) albumIdObj);
                                            }

                                            song.setArtistId(document.getString("artistId"));
                                            song.setAudioUrl(document.getString("audioUrl"));
                                            song.setCoverUrl(document.getString("coverUrl"));
                                            song.setCreateAt(document.getString("createdAt"));
                                            song.setDuration(document.getLong("duration") != null
                                                    ? document.getLong("duration").intValue()
                                                    : 0);
                                            song.setGenreId(document.getString("genreId"));
                                            song.setLikeCount(document.getLong("likeCount") != null
                                                    ? document.getLong("likeCount").intValue()
                                                    : 0);
                                            song.setTitle(document.getString("title"));
                                            song.setViewCount(document.getLong("viewCount") != null
                                                    ? document.getLong("viewCount").intValue()
                                                    : 0);

                                            songList.add(song);
                                        } catch (Exception e) {
                                            // Skip malformed documents
                                            continue;
                                        }
                                    }

                                    if (songAdapter != null) {
                                        songAdapter.notifyDataSetChanged();
                                    }
                                } else {
                                    // Handle error case
                                    if (task.getException() != null) {
                                        task.getException().printStackTrace();
                                    }
                                }
                            });
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    class SongAdapter extends ArrayAdapter<Song> {
        public SongAdapter() {
            super(getActivity(), 0, songList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_song, parent, false);
            }
            Song song = getItem(position);

            ImageView imgCover = convertView.findViewById(R.id.imgCover);
            TextView tvTitle = convertView.findViewById(R.id.tvTitle);
            TextView tvViewCount = convertView.findViewById(R.id.tvArtist);

            tvTitle.setText(song.getTitle());
            tvViewCount.setText("Lượt nghe: " + song.getViewCount());

            Glide.with(convertView.getContext())
                    .load(song.getCoverUrl())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .into(imgCover);

            return convertView;
        }
    }
}