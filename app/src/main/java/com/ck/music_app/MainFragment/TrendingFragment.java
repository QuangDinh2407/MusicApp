package com.ck.music_app.MainFragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.Adapter.TrendingSongAdapter;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Song;
import com.ck.music_app.Model.TrendingSong;
import com.ck.music_app.R;
import com.ck.music_app.utils.FirestoreUtils;
import com.ck.music_app.utils.GeminiService;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TrendingFragment - Hiển thị bài hát thịnh hành từ Gemini AI
 */
public class TrendingFragment extends Fragment {

    private RecyclerView recyclerViewGeminiTrending;
    private TrendingSongAdapter geminiAdapter;
    private List<TrendingSong> geminiTrendingSongs;
    private ProgressBar progressBarTrending;
    private TextView textViewEmptyState;
    private Button btnCountry;
    private FirebaseFirestore db;
    private GeminiService geminiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trending, container, false);

        // Khởi tạo views
        recyclerViewGeminiTrending = view.findViewById(R.id.recyclerViewGeminiTrending);
        progressBarTrending = view.findViewById(R.id.progressBarTrending);
        textViewEmptyState = view.findViewById(R.id.textViewEmptyState);
        btnCountry = view.findViewById(R.id.btnCountry);

        // Thiết lập click listener cho button country
        btnCountry.setOnClickListener(v -> showCountryMenu());

        // Khởi tạo Firebase Firestore và Gemini Service
        db = FirebaseFirestore.getInstance();
        geminiService = new GeminiService();

        // Khởi tạo danh sách và adapter
        geminiTrendingSongs = new ArrayList<>();
        geminiAdapter = new TrendingSongAdapter(getContext(), geminiTrendingSongs);

        // Thiết lập RecyclerView
        recyclerViewGeminiTrending.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewGeminiTrending.setAdapter(geminiAdapter);

        // Thiết lập click listener cho Gemini trending songs
        geminiAdapter.setOnTrendingSongClickListener((trendingSong, position) -> {
            // Tìm bài hát trong database hoặc tạo Song object tạm thời
            searchAndPlaySong(trendingSong, position);
        });

        // Test hiển thị dữ liệu mẫu ngay lập tức
        showSampleData("Việt Nam");
        
        // Tải dữ liệu từ Gemini với quốc gia mặc định (sẽ override sample data nếu thành công)
        loadGeminiTrendingSongs("Việt Nam");

        return view;
    }

    private void showCountryMenu() {
        PopupMenu popup = new PopupMenu(getContext(), btnCountry);
        popup.getMenuInflater().inflate(R.menu.menu_country, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            String selectedCountry = item.getTitle().toString();
            
            // Cập nhật text button với emoji flag
            String buttonText = getCountryWithFlag(selectedCountry);
            btnCountry.setText(buttonText);
            
            // Tải dữ liệu Gemini cho quốc gia được chọn
            loadGeminiTrendingSongs(selectedCountry);
            
            return true;
        });

        popup.show();
    }

    private String getCountryWithFlag(String country) {
        switch (country) {
            case "Việt Nam":
                return "🇻🇳 Việt Nam";
            case "Hàn Quốc":
                return "🇰🇷 Hàn Quốc";
            case "Trung Quốc":
                return "🇨🇳 Trung Quốc";
            case "Anh":
                return "🇬🇧 Anh";
            default:
                return "🌍 " + country;
        }
    }

    private void loadGeminiTrendingSongs(String country) {
        Log.d("TrendingFragment", "Loading Gemini trending songs for: " + country);
        // Hiển thị loading
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBarTrending.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Đang tải bài hát thịnh hành...", Toast.LENGTH_SHORT).show();
            });
        }
        
        geminiService.getTrendingSongs(country, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d("TrendingFragment", "Gemini response received: " + response);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBarTrending.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Đã nhận dữ liệu từ Gemini!", Toast.LENGTH_SHORT).show();
                        parseAndDisplayGeminiResponse(response);
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e("TrendingFragment", "Gemini error: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBarTrending.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_LONG).show();
                        // Hiển thị sample data khi Gemini API lỗi
                        showSampleData(country);
                    });
                }
            }
        });
    }

    private void parseAndDisplayGeminiResponse(String response) {
        try {
            Log.d("TrendingFragment", "Raw Gemini response: " + response);
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray songsArray = jsonResponse.getJSONArray("songs");
            
            Log.d("TrendingFragment", "Found " + songsArray.length() + " songs in response");
            
            // Xóa danh sách cũ trước khi thêm mới
            geminiTrendingSongs.clear();
            
            for (int i = 0; i < songsArray.length(); i++) {
                JSONObject songObj = songsArray.getJSONObject(i);
                String title = songObj.getString("title");
                String artist = songObj.getString("artist");
                int rank = songObj.getInt("rank");
                
                TrendingSong song = new TrendingSong(title, artist, rank);
                geminiTrendingSongs.add(song);
                Log.d("TrendingFragment", "Added song: " + title + " by " + artist + " rank " + rank);
            }
            
            Log.d("TrendingFragment", "Total songs in list: " + geminiTrendingSongs.size());
            
            // Kiểm tra xem adapter có null không
            if (geminiAdapter != null) {
                Log.d("TrendingFragment", "Notifying adapter to update");
                getActivity().runOnUiThread(() -> {
                    geminiAdapter.notifyDataSetChanged();
                    // Hiển thị Toast với số lượng bài hát đã load
                    Toast.makeText(getContext(), 
                        "Đã load " + geminiTrendingSongs.size() + " bài hát từ Gemini", 
                        Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.e("TrendingFragment", "Adapter is null!");
            }
            
        } catch (JSONException e) {
            Log.e("TrendingFragment", "Error parsing Gemini response: " + e.getMessage());
            e.printStackTrace();
            // Hiển thị lỗi parse cho user biết trước khi fallback
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), 
                        "Lỗi xử lý dữ liệu từ Gemini: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
            showSampleData("Việt Nam");
        }
    }

    private void showSampleData(String country) {
        Log.d("TrendingFragment", "Showing sample data for: " + country);
        geminiTrendingSongs.clear();
        
        if ("Việt Nam".equals(country)) {
            geminiTrendingSongs.add(new TrendingSong("Chúng Ta Của Hiện Tại", "Sơn Tùng M-TP", 1));
            geminiTrendingSongs.add(new TrendingSong("Waiting For You", "MONO", 2));
            geminiTrendingSongs.add(new TrendingSong("Muộn Rồi Mà Sao Còn", "Sơn Tùng M-TP", 3));
            geminiTrendingSongs.add(new TrendingSong("Người Lạ Ơi", "Karik ft Orange", 4));
            geminiTrendingSongs.add(new TrendingSong("Anh Đã Quen Với Cô Đơn", "Soobin Hoàng Sơn", 5));
            geminiTrendingSongs.add(new TrendingSong("Hãy Trao Cho Anh", "Sơn Tùng M-TP", 6));
            geminiTrendingSongs.add(new TrendingSong("Lạc Trôi", "Sơn Tùng M-TP", 7));
            geminiTrendingSongs.add(new TrendingSong("Em Của Ngày Hôm Qua", "Sơn Tùng M-TP", 8));
        } else if ("Hàn Quốc".equals(country)) {
            geminiTrendingSongs.add(new TrendingSong("Dynamite", "BTS", 1));
            geminiTrendingSongs.add(new TrendingSong("Butter", "BTS", 2));
            geminiTrendingSongs.add(new TrendingSong("Pink Venom", "BLACKPINK", 3));
            geminiTrendingSongs.add(new TrendingSong("Get Up", "NewJeans", 4));
            geminiTrendingSongs.add(new TrendingSong("LOVE DIVE", "IVE", 5));
            geminiTrendingSongs.add(new TrendingSong("Permission to Dance", "BTS", 6));
            geminiTrendingSongs.add(new TrendingSong("Shut Down", "BLACKPINK", 7));
            geminiTrendingSongs.add(new TrendingSong("Next Level", "aespa", 8));
        } else if ("Trung Quốc".equals(country)) {
            geminiTrendingSongs.add(new TrendingSong("漠河舞厅", "柳爽", 1));
            geminiTrendingSongs.add(new TrendingSong("孤勇者", "陈奕迅", 2));
            geminiTrendingSongs.add(new TrendingSong("本草纲目", "周杰伦", 3));
            geminiTrendingSongs.add(new TrendingSong("稻香", "周杰伦", 4));
            geminiTrendingSongs.add(new TrendingSong("青花瓷", "周杰伦", 5));
            geminiTrendingSongs.add(new TrendingSong("七里香", "周杰伦", 6));
            geminiTrendingSongs.add(new TrendingSong("夜曲", "周杰伦", 7));
            geminiTrendingSongs.add(new TrendingSong("告白气球", "周杰伦", 8));
        } else if ("Anh".equals(country)) {
            geminiTrendingSongs.add(new TrendingSong("As It Was", "Harry Styles", 1));
            geminiTrendingSongs.add(new TrendingSong("Heat Waves", "Glass Animals", 2));
            geminiTrendingSongs.add(new TrendingSong("Bad Habits", "Ed Sheeran", 3));
            geminiTrendingSongs.add(new TrendingSong("Stay", "The Kid LAROI & Justin Bieber", 4));
            geminiTrendingSongs.add(new TrendingSong("Good 4 U", "Olivia Rodrigo", 5));
            geminiTrendingSongs.add(new TrendingSong("Flowers", "Miley Cyrus", 6));
            geminiTrendingSongs.add(new TrendingSong("Anti-Hero", "Taylor Swift", 7));
            geminiTrendingSongs.add(new TrendingSong("Unholy", "Sam Smith ft. Kim Petras", 8));
        }
        
        Log.d("TrendingFragment", "Added " + geminiTrendingSongs.size() + " sample songs");
        geminiAdapter.notifyDataSetChanged();
    }

    private void searchAndPlaySong(TrendingSong trendingSong, int position) {
        Log.d("TrendingFragment", "Searching for song: " + trendingSong.getTitle() + " by " + trendingSong.getArtist());
        
        // Tìm kiếm bài hát chính xác trong database
        db.collection("songs")
                .whereEqualTo("title", trendingSong.getTitle())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Tìm thấy bài hát chính xác
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Song song = document.toObject(Song.class);
                        song.setSongId(document.getId());
                        
                        // Tạo playlist với bài hát này và phát nhạc
                        List<Song> playlist = Arrays.asList(song);
                        if (getActivity() instanceof MainActivity) {
                            MainActivity mainActivity = (MainActivity) getActivity();
                            mainActivity.showPlayer(playlist, 0, "Thịnh hành - " + trendingSong.getTitle());
                        }
                        
                        Toast.makeText(getContext(), "Đang phát: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        // Không tìm thấy bài hát chính xác, tìm kiếm tương tự
                        searchSimilarSongs(trendingSong, position);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TrendingFragment", "Error searching for song", e);
                    Toast.makeText(getContext(), "Lỗi khi tìm bài hát", Toast.LENGTH_SHORT).show();
                    playAvailableSongs(position);
                });
    }

    private void searchSimilarSongs(TrendingSong trendingSong, int position) {
        // Tách từ khóa từ tên bài hát và tìm kiếm fuzzy
        String[] keywords = trendingSong.getTitle().toLowerCase().split(" ");
        
        // Tìm kiếm bài hát có chứa từ khóa
        db.collection("songs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Song> similarSongs = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Song song = document.toObject(Song.class);
                        song.setSongId(document.getId());
                        
                         String songTitle = song.getTitle().toLowerCase();
                         String songArtistId = song.getArtistId() != null ? song.getArtistId().toLowerCase() : "";
                         String targetArtist = trendingSong.getArtist().toLowerCase();
                         
                         // Kiểm tra nếu có từ khóa khớp trong title hoặc artist
                         boolean hasMatchingKeyword = false;
                         for (String keyword : keywords) {
                             if (songTitle.contains(keyword) || songArtistId.contains(keyword) || 
                                 songArtistId.contains(targetArtist) || targetArtist.contains(songArtistId)) {
                                 hasMatchingKeyword = true;
                                 break;
                             }
                         }
                        
                        if (hasMatchingKeyword) {
                            similarSongs.add(song);
                        }
                    }
                    
                    if (!similarSongs.isEmpty()) {
                        // Phát bài hát tương tự đầu tiên
                        if (getActivity() instanceof MainActivity) {
                            MainActivity mainActivity = (MainActivity) getActivity();
                            mainActivity.showPlayer(similarSongs, 0, "Bài hát tương tự - " + trendingSong.getTitle());
                        }
                        
                        Toast.makeText(getContext(), "Phát bài hát tương tự: " + similarSongs.get(0).getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        // Không tìm thấy bài hát tương tự, phát playlist có sẵn
                        playAvailableSongs(position);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TrendingFragment", "Error searching similar songs", e);
                    playAvailableSongs(position);
                });
    }

    private void playAvailableSongs(int position) {
        // Fallback: Lấy bất kỳ bài hát nào có sẵn từ database
        FirestoreUtils.getAllSongs(new FirestoreUtils.FirestoreCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> songs) {
                if (!songs.isEmpty()) {
                    // Phát từ vị trí được chọn hoặc bài đầu tiên
                    int playIndex = Math.min(position, songs.size() - 1);
                    
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.showPlayer(songs, playIndex, "Nhạc có sẵn");
                    }
                    
                    Toast.makeText(getContext(), "Phát danh sách nhạc có sẵn", Toast.LENGTH_SHORT).show();
                } else {
                    // Nếu không có bài hát nào, tạo demo playlist
                    playDemoPlaylistFromPosition(position);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("TrendingFragment", "Error loading available songs", e);
                playDemoPlaylistFromPosition(position);
            }
        });
    }

    private void playDemoPlaylistFromPosition(int position) {
        // Tạo demo playlist từ trending songs
        List<Song> demoPlaylist = new ArrayList<>();
        
        for (TrendingSong trendingSong : geminiTrendingSongs) {
            Song demoSong = createDemoSongFromTrending(trendingSong);
            demoPlaylist.add(demoSong);
        }
        
        if (!demoPlaylist.isEmpty()) {
            int playIndex = Math.min(position, demoPlaylist.size() - 1);
            
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.showPlayer(demoPlaylist, playIndex, "Demo Playlist");
            }
            
            Toast.makeText(getContext(), "Phát demo playlist", Toast.LENGTH_SHORT).show();
        }
    }

    private Song createDemoSongFromTrending(TrendingSong trendingSong) {
        Song demoSong = new Song();
        demoSong.setTitle(trendingSong.getTitle());
        demoSong.setArtist(trendingSong.getArtist());
        demoSong.setImageUrl("https://via.placeholder.com/300x300.png?text=" + trendingSong.getTitle().replace(" ", "+"));
        demoSong.setAudioUrl(getDemoAudioUrl(trendingSong));
        demoSong.setDurationFromString("3:30"); // Demo duration
        demoSong.setGenre("Pop");
        demoSong.setAlbum("Trending Album");
        demoSong.setYear("2024");
        demoSong.setViewCountFromLong(1000000L + trendingSong.getRank() * 100000L);
        demoSong.setSongId("demo_" + trendingSong.getRank());
        return demoSong;
    }

    private String getDemoAudioUrl(TrendingSong trendingSong) {
        // Trả về URL demo audio hoặc URL mặc định
        // Trong thực tế, bạn có thể sử dụng free music API hoặc sample audio files
        return "https://www.soundjay.com/misc/sounds-mp3/bell-ringing-05.mp3"; // Demo URL
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TrendingFragment", "Fragment resumed");
        
        // Refresh data khi fragment được hiển thị lại
        if (geminiTrendingSongs.isEmpty()) {
            showSampleData("Việt Nam");
        }
    }
} 