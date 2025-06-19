package com.ck.music_app.Activity;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.ck.music_app.Adapter.PlaylistSongAdapter;
import com.ck.music_app.MainActivity;
import com.ck.music_app.MainFragment.MusicPlayerFragment;
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.FirebaseService;
import com.ck.music_app.Services.MusicService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity implements PlaylistSongAdapter.OnSongActionListener {
    private Playlist playlist;
    private FirebaseFirestore db;
    private List<Song> songs = new ArrayList<>();
    private PlaylistSongAdapter adapter;
    
    private ImageView imgPlaylistCover;
    private ImageView imgBackground;
    private TextView tvPlaylistName;
    private TextView tvSongCount;
    private EditText etSearch;
    private RecyclerView rvSongs;
    private MaterialButton btnAddSongs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        // Lấy playlistId từ intent
        String playlistId = getIntent().getStringExtra("playlistId");
        if (playlistId == null) {
            Toast.makeText(this, "Không thể tải thông tin playlist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupAddSongsButton();

        // Truy vấn Firestore để lấy thông tin playlist
        db.collection("playlists").document(playlistId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                playlist = documentSnapshot.toObject(Playlist.class);
                if (playlist == null) {
                    Toast.makeText(this, "Không tìm thấy playlist", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                playlist.setId(documentSnapshot.getId());
                
                // Set UI
                tvPlaylistName.setText(playlist.getName());
                
                // Load cover image
                String coverUrl = playlist.getCoverUrl();
                Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.love)
                    .error(R.drawable.love)
                    .into(imgPlaylistCover);

                // Load background image with blur
                Glide.with(this)
                    .load(coverUrl)
                    .transform(
                        new MultiTransformation<>(
                            new CenterCrop(),
                            new com.bumptech.glide.load.resource.bitmap.BitmapTransformation() {
                                @Override
                                protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
                                    Bitmap blurred = Bitmap.createBitmap(toTransform.getWidth(), toTransform.getHeight(), Bitmap.Config.ARGB_8888);
                                    Canvas canvas = new Canvas(blurred);
                                    Paint paint = new Paint();
                                    paint.setAlpha(180);
                                    canvas.drawBitmap(toTransform, 0, 0, paint);

                                    RenderScript rs = RenderScript.create(PlaylistDetailActivity.this);
                                    Allocation input = Allocation.createFromBitmap(rs, blurred);
                                    Allocation output = Allocation.createTyped(rs, input.getType());
                                    ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                                    script.setRadius(25f);
                                    script.setInput(input);
                                    script.forEach(output);
                                    output.copyTo(blurred);
                                    rs.destroy();

                                    return blurred;
                                }

                                @Override
                                public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
                                    messageDigest.update("blur transformation".getBytes());
                                }
                            }
                        )
                    )
                    .placeholder(R.drawable.love)
                    .error(R.drawable.love)
                    .into(imgBackground);
                
                // Load songs
                loadPlaylistSongs();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi khi tải playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void initializeViews() {
        imgPlaylistCover = findViewById(R.id.imgPlaylistCover);
        imgBackground = findViewById(R.id.imgBackground);
        tvPlaylistName = findViewById(R.id.tvPlaylistName);
        tvSongCount = findViewById(R.id.tvSongCount);
        etSearch = findViewById(R.id.etSearch);
        rvSongs = findViewById(R.id.rvSongs);
        btnAddSongs = findViewById(R.id.btnAddSongs);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
    }

    private void setupRecyclerView() {
        adapter = new PlaylistSongAdapter(this, songs, this);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        rvSongs.setAdapter(adapter);
    }

    private void loadPlaylistSongs() {
        if (playlist.getSongIds() == null || playlist.getSongIds().isEmpty()) {
            updateSongCount(0);
            return;
        }

        songs.clear(); // Clear existing songs before loading

        // Keep track of whether we've updated the cover
        final boolean[] coverUpdated = {false};

        for (String songId : playlist.getSongIds()) {
            if (songId == null || songId.isEmpty()) {
                continue; // Skip null or empty song IDs
            }

            db.collection("songs").document(songId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Song song = documentSnapshot.toObject(Song.class);
                    if (song != null) {
                        song.setSongId(documentSnapshot.getId()); // Ensure song has its ID
                        songs.add(song);
                        adapter.notifyItemInserted(songs.size() - 1);
                        updateSongCount(songs.size());

                        // Update playlist cover with first song's image
                        if (!coverUpdated[0] && song.getCoverUrl() != null && !song.getCoverUrl().isEmpty()) {
                            coverUpdated[0] = true;
                            // Update cover in UI
                            Glide.with(this)
                                .load(song.getCoverUrl())
                                .placeholder(R.drawable.love)
                                .error(R.drawable.love)
                                .into(imgPlaylistCover);

                            // Update cover in database
                            db.collection("playlists").document(playlist.getId())
                                .update("coverUrl", song.getCoverUrl())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("PlaylistDetail", "Successfully updated playlist cover");
                                    // Send broadcast to update playlist list
                                    Intent intent = new Intent("PLAYLIST_UPDATED");
                                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("PlaylistDetail", "Error updating playlist cover", e);
                                });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PlaylistDetail", "Error loading song: " + songId, e);
                });
        }
    }

    private void updateSongCount(int count) {
        tvSongCount.setText(count + " bài hát");
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                // Ẩn nút thêm bài hát khi đang tìm kiếm
                btnAddSongs.setVisibility(query.isEmpty() ? View.VISIBLE : View.GONE);
                
                if (query.isEmpty()) {
                    adapter.updateSongs(songs);
                } else {
                    List<Song> filteredSongs = new ArrayList<>();
                    for (Song song : songs) {
                        if (song.getTitle().toLowerCase().contains(query)) {
                            filteredSongs.add(song);
                        }
                    }
                    adapter.updateSongs(filteredSongs);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupAddSongsButton() {
        btnAddSongs.setOnClickListener(v -> showAddSongsDialog());
    }

    private void showAddSongsDialog() {
        // Khởi tạo songIds nếu null
        if (playlist.getSongIds() == null) {
            playlist.setSongIds(new ArrayList<>());
        }

        // Query all songs not in playlist
        Query query = db.collection("songs");
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Song> allSongs = new ArrayList<>();
            List<Song> filteredSongs = new ArrayList<>();
            List<Song> songsToAdd = new ArrayList<>();
            
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                Song song = document.toObject(Song.class);
                if (song != null) {
                    song.setSongId(document.getId()); // Set song ID
                    if (!playlist.getSongIds().contains(document.getId())) {
                        allSongs.add(song);
                        filteredSongs.add(song); // Khởi tạo danh sách lọc với tất cả bài hát
                    }
                }
            }

            // Tạo AlertDialog với ListView custom
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LightDialogTheme);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_songs, null);
            TextView tvSelectedCount = dialogView.findViewById(R.id.tvSelectedCount);
            EditText etSearchSongs = dialogView.findViewById(R.id.etSearchSongs);
            ListView listView = dialogView.findViewById(R.id.listViewSongs);

            // Cập nhật số lượng bài hát đã chọn
            tvSelectedCount.setText("Đã chọn: 0");

            // Tạo adapter cho ListView với layout tùy chỉnh
            ArrayAdapter<Song> adapter = new ArrayAdapter<Song>(this, 0, filteredSongs) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.item_song_select, parent, false);
                    }

                    Song song = getItem(position);
                    if (song != null) {
                        ImageView imgSongCover = convertView.findViewById(R.id.imgSongCover);
                        TextView tvSongTitle = convertView.findViewById(R.id.tvSongTitle);
                        TextView tvArtistName = convertView.findViewById(R.id.tvArtistName);
                        View selectedBackground = convertView.findViewById(R.id.selectedBackground);

                        tvSongTitle.setText(song.getTitle());
                        // Lấy tên artist từ artistId
                        FirebaseService.getInstance().getArtistNameById(song.getArtistId(), new FirebaseService.FirestoreCallback<String>() {
                            @Override
                            public void onSuccess(String artistName) {
                                tvArtistName.setText(artistName);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Nếu không lấy được tên artist, hiển thị "Unknown Artist"
                                tvArtistName.setText("Unknown Artist");
                                Log.e("PlaylistDetail", "Error getting artist name: " + e.getMessage());
                            }
                        });
                        
                        // Load ảnh bài hát
                        Glide.with(PlaylistDetailActivity.this)
                            .load(song.getCoverUrl())
                            .placeholder(R.drawable.love)
                            .error(R.drawable.love)
                            .into(imgSongCover);

                        // Hiển thị background mờ nếu item được chọn
                        selectedBackground.setVisibility(songsToAdd.contains(song) ? View.VISIBLE : View.GONE);
                    }

                    return convertView;
                }
            };

            listView.setAdapter(adapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            
            // Xử lý sự kiện khi người dùng chọn/bỏ chọn bài hát
            listView.setOnItemClickListener((parent, view, position, id) -> {
                Song song = filteredSongs.get(position);
                View selectedBackground = view.findViewById(R.id.selectedBackground);
                
                if (songsToAdd.contains(song)) {
                    songsToAdd.remove(song);
                    selectedBackground.setVisibility(View.GONE);
                } else {
                    songsToAdd.add(song);
                    selectedBackground.setVisibility(View.VISIBLE);
                }
                
                // Cập nhật số lượng bài hát đã chọn
                tvSelectedCount.setText("Đã chọn: " + songsToAdd.size());
            });

            // Xử lý sự kiện tìm kiếm
            etSearchSongs.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String searchText = s.toString().toLowerCase().trim();
                    filteredSongs.clear();
                    
                    if (searchText.isEmpty()) {
                        filteredSongs.addAll(allSongs);
                    } else {
                        for (Song song : allSongs) {
                            if (song.getTitle().toLowerCase().contains(searchText) ||
                                song.getArtistId().toLowerCase().contains(searchText)) {
                                filteredSongs.add(song);
                            }
                        }
                    }
                    
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            builder.setView(dialogView);
            
            builder.setPositiveButton("Thêm", (dialog, which) -> {
                if (!songsToAdd.isEmpty()) {
                    addSongsToPlaylist(songsToAdd);
                }
            });
            
            builder.setNegativeButton("Hủy", null);
            
            AlertDialog dialog = builder.create();
            dialog.show();
        }).addOnFailureListener(e -> {
            Log.e("PlaylistDetail", "Error loading songs: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi tải danh sách bài hát", Toast.LENGTH_SHORT).show();
        });
    }

    private void addSongsToPlaylist(List<Song> songsToAdd) {
        try {
            // Khởi tạo danh sách nếu null
            if (playlist.getSongIds() == null) {
                playlist.setSongIds(new ArrayList<>());
            }

            // Tạo danh sách mới từ danh sách hiện tại
            List<String> newSongIds = new ArrayList<>(playlist.getSongIds());
            boolean wasEmpty = newSongIds.isEmpty();
            
            // Log trạng thái hiện tại
            Log.d("PlaylistDetail", "Current songIds before adding: " + newSongIds);

            // Thêm ID của các bài hát mới
            for (Song song : songsToAdd) {
                String songId = song.getSongId();
                if (songId != null && !songId.isEmpty() && !newSongIds.contains(songId)) {
                    newSongIds.add(songId);
                    Log.d("PlaylistDetail", "Adding song ID: " + songId);
                }
            }

            // Log để debug
            Log.d("PlaylistDetail", "Playlist ID: " + playlist.getId());
            Log.d("PlaylistDetail", "Final songIds to save: " + newSongIds);

            // Tạo map dữ liệu để update
            HashMap<String, Object> playlistData = new HashMap<>();
            playlistData.put("songIds", newSongIds);
            playlistData.put("name", playlist.getName());
            
            // Nếu playlist đang trống và đang thêm bài hát mới, cập nhật ảnh bìa
            if (wasEmpty && !songsToAdd.isEmpty()) {
                String newCoverUrl = songsToAdd.get(0).getCoverUrl();
                playlistData.put("coverUrl", newCoverUrl);
                Log.d("PlaylistDetail", "Updating cover URL to: " + newCoverUrl);
            } else {
                playlistData.put("coverUrl", playlist.getCoverUrl());
            }
            
            playlistData.put("createdAt", playlist.getCreatedAt());

            // Cập nhật trực tiếp vào Firestore
            db.collection("playlists")
                .document(playlist.getId())
                .set(playlistData)
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật local playlist
                    playlist.setSongIds(newSongIds);
                    
                    // Cập nhật ảnh bìa nếu playlist đang trống
                    if (wasEmpty && !songsToAdd.isEmpty()) {
                        String newCoverUrl = songsToAdd.get(0).getCoverUrl();
                        playlist.setCoverUrl(newCoverUrl);
                        // Cập nhật UI ảnh bìa ngay lập tức
                        Glide.with(this)
                            .load(newCoverUrl)
                            .placeholder(R.drawable.love)
                            .error(R.drawable.love)
                            .into(imgPlaylistCover);
                    }
                    
                    songs.addAll(songsToAdd);
                    adapter.notifyDataSetChanged();
                    updateSongCount(songs.size());

                    // Kiểm tra lại dữ liệu
                    verifyPlaylistData(playlist.getId());

                    // Gửi broadcast để cập nhật danh sách playlist
                    Intent intent = new Intent("PLAYLIST_UPDATED");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                    Toast.makeText(PlaylistDetailActivity.this, 
                        "Đã thêm " + songsToAdd.size() + " bài hát", 
                        Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("PlaylistDetail", "Error saving playlist: " + e.getMessage());
                    Toast.makeText(PlaylistDetailActivity.this, 
                        "Lỗi khi lưu playlist: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });

        } catch (Exception e) {
            Log.e("PlaylistDetail", "Error in addSongsToPlaylist: " + e.getMessage());
            Toast.makeText(this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyPlaylistData(String playlistId) {
        db.collection("playlists")
            .document(playlistId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    List<String> savedSongIds = (List<String>) document.get("songIds");
                    Log.d("PlaylistDetail", "Verified data in Firestore - songIds: " + 
                        (savedSongIds != null ? savedSongIds.toString() : "null"));
                    
                    if (savedSongIds == null || savedSongIds.isEmpty()) {
                        Log.e("PlaylistDetail", "Warning: Saved songIds is null or empty!");
                    }
                } else {
                    Log.e("PlaylistDetail", "Error: Playlist document doesn't exist!");
                }
            })
            .addOnFailureListener(e -> 
                Log.e("PlaylistDetail", "Error verifying playlist data: " + e.getMessage())
            );
    }

    @Override
    public void onSongClick(Song song) {
        // Tìm vị trí của bài hát được click trong danh sách
        int position = songs.indexOf(song);

        // Chuyển về MainActivity và hiển thị player ngay lập tức
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("songList", new ArrayList<>(songs));
        mainIntent.putExtra("position", position);
        mainIntent.putExtra("albumName", playlist.getName());
        mainIntent.putExtra("showPlayer", true);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(mainIntent);
        finish();
    }

    @Override
    public void onRemoveClick(Song song, int position) {
        List<String> currentSongIds = new ArrayList<>(playlist.getSongIds());
        currentSongIds.remove(song.getSongId());
        
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("songIds", currentSongIds);

        // Xác định ảnh bìa mới nếu cần
        String newCoverUrl;
        if (currentSongIds.isEmpty()) {
            // Playlist trống, dùng ảnh mặc định
            newCoverUrl = "https://img.freepik.com/free-photo/digital-illustration-simple-blue-heart_181624-33760.jpg?semt=ais_hybrid&w=740";
        } else if (position == 0) {
            // Nếu xóa bài đầu tiên, lấy ảnh của bài hát mới đầu tiên
            Song newFirstSong = songs.get(1); // Bài hát thứ 2 sẽ trở thành bài đầu tiên
            newCoverUrl = newFirstSong.getCoverUrl();
        } else {
            newCoverUrl = null;
        }

        // Chỉ cập nhật coverUrl nếu cần thiết
        if (newCoverUrl != null) {
            updates.put("coverUrl", newCoverUrl);
        }

        db.collection("playlists").document(playlist.getId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                // Cập nhật dữ liệu local
                playlist.setSongIds(currentSongIds);
                
                // Cập nhật ảnh bìa nếu cần
                if (newCoverUrl != null) {
                    playlist.setCoverUrl(newCoverUrl);
                    Glide.with(this)
                        .load(newCoverUrl)
                        .placeholder(R.drawable.love)
                        .error(R.drawable.love)
                        .into(imgPlaylistCover);
                }

                // Cập nhật UI
                songs.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, songs.size());
                updateSongCount(songs.size());

                // Thông báo cho các component khác
                Intent intent = new Intent("PLAYLIST_UPDATED");
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                Toast.makeText(this, "Đã xóa bài hát khỏi playlist", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi khi cập nhật playlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onMoreClick(Song song, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.popup_playlist_song);
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_remove) {
                // Tìm vị trí của bài hát trong danh sách
                int position = songs.indexOf(song);
                if (position != -1) {
                    onRemoveClick(song, position);
                }
                return true;
            } else if (id == R.id.action_change) {
                // Tìm vị trí của bài hát cần thay thế
                int position = songs.indexOf(song);
                if (position != -1) {
                    showChangeSongDialog(song, position);
                }
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void showChangeSongDialog(Song oldSong, int position) {
        // Query tất cả các bài hát không có trong playlist
        Query query = db.collection("songs");
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Song> availableSongs = new ArrayList<>();
            
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                Song song = document.toObject(Song.class);
                if (song != null) {
                    song.setSongId(document.getId());
                    // Chỉ thêm những bài hát không có trong playlist hoặc không phải bài hát hiện tại
                    if (!playlist.getSongIds().contains(document.getId()) || 
                        document.getId().equals(oldSong.getSongId())) {
                        availableSongs.add(song);
                    }
                }
            }

            // Tạo AlertDialog với ListView custom
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LightDialogTheme);
            builder.setTitle("Chọn bài hát thay thế");

            // Tạo adapter cho ListView
            ArrayAdapter<Song> adapter = new ArrayAdapter<Song>(this, 0, availableSongs) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.item_song_select, parent, false);
                    }

                    Song song = getItem(position);
                    if (song != null) {
                        ImageView imgSongCover = convertView.findViewById(R.id.imgSongCover);
                        TextView tvSongTitle = convertView.findViewById(R.id.tvSongTitle);

                        tvSongTitle.setText(song.getTitle());
                        
                        // Load ảnh bài hát
                        Glide.with(PlaylistDetailActivity.this)
                            .load(song.getCoverUrl())
                            .placeholder(R.drawable.love)
                            .error(R.drawable.love)
                            .into(imgSongCover);
                    }

                    return convertView;
                }
            };

            builder.setAdapter(adapter, (dialog, which) -> {
                Song newSong = availableSongs.get(which);
                replaceSongInPlaylist(oldSong, newSong, position);
            });

            builder.setNegativeButton("Hủy", null);
            builder.show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi khi tải danh sách bài hát", Toast.LENGTH_SHORT).show();
        });
    }

    private void replaceSongInPlaylist(Song oldSong, Song newSong, int position) {
        // Tạo danh sách mới từ danh sách hiện tại
        List<String> newSongIds = new ArrayList<>(playlist.getSongIds());
        
        // Thay thế ID bài hát cũ bằng ID bài hát mới
        int songIndex = newSongIds.indexOf(oldSong.getSongId());
        if (songIndex != -1) {
            newSongIds.set(songIndex, newSong.getSongId());
            
            // Cập nhật Firestore
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("songIds", newSongIds);

            // Nếu thay thế bài hát đầu tiên, cập nhật ảnh bìa playlist
            if (position == 0) {
                updates.put("coverUrl", newSong.getCoverUrl());
            }

            db.collection("playlists").document(playlist.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật dữ liệu local
                    playlist.setSongIds(newSongIds);
                    
                    // Cập nhật ảnh bìa nếu là bài hát đầu tiên
                    if (position == 0) {
                        playlist.setCoverUrl(newSong.getCoverUrl());
                        Glide.with(this)
                            .load(newSong.getCoverUrl())
                            .placeholder(R.drawable.love)
                            .error(R.drawable.love)
                            .into(imgPlaylistCover);
                    }

                    // Cập nhật UI
                    songs.set(position, newSong);
                    adapter.notifyItemChanged(position);

                    // Thông báo cho các component khác
                    Intent intent = new Intent("PLAYLIST_UPDATED");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    
                    Toast.makeText(this, 
                        "Đã thay thế bài hát thành " + newSong.getTitle(), 
                        Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, 
                        "Lỗi khi cập nhật playlist: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Force refresh playlist list when activity is destroyed
        Intent intent = new Intent("PLAYLIST_UPDATED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void notifyPlaylistSongsChanged() {
        if (playlist != null) {
            Intent intent = new Intent("PLAYLIST_SONGS_CHANGED");
            intent.putExtra("playlistId", playlist.getId());
            intent.putStringArrayListExtra("songIds", new ArrayList<>(playlist.getSongIds()));
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d("PlaylistDetail", "Broadcasting songs changed for playlist: " + playlist.getId());
        }
    }


} 