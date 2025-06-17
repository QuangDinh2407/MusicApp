package com.ck.music_app.MainFragment.ProfileChildFragment;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.ck.music_app.Adapter.LocalSongAdapter;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.LocalSong;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.MusicService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DownloadedFragment extends Fragment implements LocalSongAdapter.OnSongClickListener {
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private RecyclerView rvLocalSongs;
    private LocalSongAdapter adapter;
    private List<LocalSong> localSongs = new ArrayList<>();

    private OnFragmentDismissListener dismissListener;

    public interface OnFragmentDismissListener {
        void onDismiss();
    }

    public void setOnFragmentDismissListener(OnFragmentDismissListener listener) {
        this.dismissListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downloaded, container, false);
        rvLocalSongs = view.findViewById(R.id.rvLocalSongs);
        rvLocalSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Thêm divider cho RecyclerView
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_item));
        rvLocalSongs.addItemDecoration(dividerItemDecoration);

        // Kiểm tra và yêu cầu quyền, load danh sách bài hát
        checkAndRequestPermissions();

        // Nút back
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                    .remove(this)
                    .commit();
                getParentFragmentManager().popBackStack();
            }
        });

        // Nút phát ngẫu nhiên
        TextView btnPlayRandom = view.findViewById(R.id.btnPlayRandom);
        btnPlayRandom.setOnClickListener(v -> playRandomSong());

        // Nút quét lại
        Button btnRescan = view.findViewById(R.id.btnRescan);
        btnRescan.setOnClickListener(v -> {
            checkAndRequestPermissions();
            Toast.makeText(getContext(), "Đã quét lại nhạc trong thiết bị", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void checkAndRequestPermissions() {
        Context context = getContext();
        if (context == null) return;

        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            // Nếu đã từ chối vĩnh viễn
            if (!shouldShowRequestPermissionRationale(permission)) {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Cấp quyền truy cập nhạc")
                    .setMessage("Bạn đã từ chối quyền truy cập nhạc. Vui lòng vào Cài đặt > Ứng dụng > Quyền để cấp lại quyền cho ứng dụng.")
                    .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Đóng", null)
                    .show();
            } else {
                // Gọi requestPermissions trực tiếp từ Fragment
                requestPermissions(new String[]{permission}, REQUEST_CODE_PERMISSIONS);
                Toast.makeText(context, "Vui lòng cấp quyền để truy cập nhạc", Toast.LENGTH_SHORT).show();
            }
        } else {
            loadLocalSongs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadLocalSongs();
            } else {
                Toast.makeText(getContext(), "Quyền truy cập bị từ chối. Không thể tải danh sách nhạc.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadLocalSongs() {
        localSongs.clear();
        Context context = getContext();
        if (context == null) {
            Log.e("DownloadedFragment", "Context is null");
            return;
        }

        // Sử dụng VOLUME_EXTERNAL để hỗ trợ cả bộ nhớ trong và ngoài
        Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) :
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA, // Thêm DATA để lấy đường dẫn file
                MediaStore.Audio.Media.RELATIVE_PATH // Thêm RELATIVE_PATH để lấy đường dẫn tương đối
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0" + " AND " +
                MediaStore.Audio.Media.DURATION + " >= 30000"; // Lọc file dài hơn 30 giây
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor = context.getContentResolver().query(collection, projection, selection, null, sortOrder)) {
            if (cursor == null) {
                Log.e("DownloadedFragment", "Cursor is null");
                Toast.makeText(context, "Không thể truy vấn danh sách nhạc", Toast.LENGTH_SHORT).show();
                return;
            }

            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int relativePathCol = -1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                relativePathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH);
            }

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String metadataArtist = cursor.getString(artistCol);
                long duration = cursor.getLong(durationCol);
                String filePath = cursor.getString(dataCol);
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                Log.d("DownloadedFragment", "Processing song: " + title);
                Log.d("DownloadedFragment", "File path: " + filePath);

                // Lấy artist từ cấu trúc thư mục
                String artist = metadataArtist;
                File file = new File(filePath);
                File parentDir = file.getParentFile();
                
                if (parentDir != null) {
                    String dirName = parentDir.getName();
                    // Kiểm tra xem thư mục cha có phải là Music không
                    if (!dirName.equals("Music") && !dirName.equals("music")) {
                        // Nếu không phải thư mục Music, thì đây có thể là thư mục artist
                        Log.d("DownloadedFragment", "Found artist from directory: " + dirName);
                        if (dirName.equals("Unknown_Artist")) {
                            // Nếu là Unknown_Artist, thử dùng metadata artist
                            artist = metadataArtist != null ? metadataArtist : "Unknown Artist";
                        } else {
                            // Sử dụng tên thư mục làm tên artist
                            artist = dirName;
                        }
                    }
                }

                // Mặc định sử dụng ảnh mipmap/ic_launcher_new
                String coverUrl = "android.resource://" + context.getPackageName() + "/mipmap/ic_launcher_new";
                String lyrics = null;

                // Thử lấy ảnh và lyrics từ metadata của file nhạc
                try {
                    android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                    retriever.setDataSource(filePath);
                    
                    // 1. Thử lấy ảnh từ metadata của file nhạc
                    byte[] art = retriever.getEmbeddedPicture();
                    if (art != null) {
                        Log.d("DownloadedFragment", "Found embedded art for: " + title + ", size: " + art.length + " bytes");
                        
                        // Lưu ảnh vào cache
                        File cacheDir = new File(context.getCacheDir(), "album_art");
                        if (!cacheDir.exists()) {
                            boolean created = cacheDir.mkdirs();
                            Log.d("DownloadedFragment", "Created cache dir: " + created);
                        }
                        
                        File coverFile = new File(cacheDir, "cover_" + id + ".jpg");
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(coverFile)) {
                            fos.write(art);
                            coverUrl = Uri.fromFile(coverFile).toString();
                            Log.d("DownloadedFragment", "Saved embedded art to: " + coverUrl);
                        } catch (Exception e) {
                            Log.e("DownloadedFragment", "Error saving embedded art: " + e.getMessage());
                        }
                    } else {
                        Log.d("DownloadedFragment", "No embedded art found for: " + title + ", searching for external images...");
                        
                        // 2. Thử tìm file ảnh trong cùng thư mục với file nhạc
                        File musicFile = new File(filePath);
                        File musicDir = musicFile.getParentFile();
                        String baseName = filePath.substring(0, filePath.lastIndexOf("."));
                        
                        // Danh sách các định dạng ảnh phổ biến cần tìm
                        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".webp"};
                        
                        // Danh sách các tên file ảnh có thể có
                        String[] possibleNames = {
                            baseName,  // Cùng tên với file nhạc
                            baseName + "_cover",
                            baseName + "_album",
                            baseName + "_art",
                            new File(baseName).getName(), // Chỉ tên file không có path
                            "cover",
                            "album",
                            "folder"
                        };

                        // Tìm theo tất cả các khả năng
                        boolean found = false;
                        for (String name : possibleNames) {
                            for (String ext : imageExtensions) {
                                File imageFile = new File(musicDir, name + ext);
                                if (imageFile.exists()) {
                                    coverUrl = Uri.fromFile(imageFile).toString();
                                    Log.d("DownloadedFragment", "Found external image: " + coverUrl);
                                    found = true;
                                    break;
                                }
                            }
                            if (found) break;
                        }
                        
                        if (!found) {
                            // 3. Thử tìm trong thư mục cha (album/artist folder)
                            File artistParentDir = musicDir.getParentFile();
                            if (artistParentDir != null) {
                                for (String ext : imageExtensions) {
                                    File imageFile = new File(artistParentDir, "cover" + ext);
                                    if (imageFile.exists()) {
                                        coverUrl = Uri.fromFile(imageFile).toString();
                                        Log.d("DownloadedFragment", "Found cover in parent dir: " + coverUrl);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (!found) {
                            Log.d("DownloadedFragment", "No external images found for: " + title);
                        }
                    }
                    
                    // Thử lấy lyrics từ các metadata khác nhau
                    lyrics = getLyricsFromFile(filePath);
                    if (lyrics == null || lyrics.isEmpty()) {
                        // Nếu không có file lyrics, tạo lyrics cơ bản từ metadata
                        StringBuilder sb = new StringBuilder();
                        String songTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String songArtist = artist; // Sử dụng artist đã xác định ở trên
                        String songAlbum = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM);
                        String songYear = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR);
                        
                        if (songTitle != null) sb.append("Title: ").append(songTitle).append("\n");
                        if (songArtist != null) sb.append("Artist: ").append(songArtist).append("\n");
                        if (songAlbum != null) sb.append("Album: ").append(songAlbum).append("\n");
                        if (songYear != null) sb.append("Year: ").append(songYear).append("\n");
                        
                        lyrics = sb.toString();
                    }
                    
                    retriever.release();
                } catch (Exception e) {
                    Log.e("DownloadedFragment", "Error extracting metadata for " + title + ": " + e.getMessage());
                }

                Log.d("DownloadedFragment", "Final cover URL for " + title + ": " + coverUrl);
                Log.d("DownloadedFragment", "Found song: " + title + " - " + artist + " - " + uri.toString());
                localSongs.add(new LocalSong(title, artist, duration, uri, context, coverUrl, lyrics));
            }

            Log.d("DownloadedFragment", "Total songs found: " + localSongs.size());
        } catch (Exception e) {
            Log.e("DownloadedFragment", "Error querying MediaStore: " + e.getMessage());
            Toast.makeText(context, "Lỗi khi tải danh sách nhạc", Toast.LENGTH_SHORT).show();
        }

        if (localSongs.isEmpty()) {
            Log.w("DownloadedFragment", "Không tìm thấy bài hát nào!");
            Toast.makeText(context, "Không tìm thấy bài hát nào trong thiết bị", Toast.LENGTH_SHORT).show();
        }

        // Khởi tạo adapter sau khi đã load xong danh sách bài hát
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                adapter = new LocalSongAdapter(requireContext(), localSongs, this);
                rvLocalSongs.setAdapter(adapter);
            });
        } else {
            Log.e("DownloadedFragment", "Activity is null, cannot set adapter!");
        }
    }

    private String getLyricsFromFile(String audioFilePath) {
        try {
            // Thử tìm file .lrc cùng tên với file nhạc
            String lrcPath = audioFilePath.substring(0, audioFilePath.lastIndexOf(".")) + ".lrc";
            File lrcFile = new File(lrcPath);
            
            // Thử tìm file .txt cùng tên với file nhạc
            String txtPath = audioFilePath.substring(0, audioFilePath.lastIndexOf(".")) + ".txt";
            File txtFile = new File(txtPath);

            // Đọc nội dung file lyrics nếu tồn tại
            if (lrcFile.exists()) {
                return readFile(lrcFile);
            } else if (txtFile.exists()) {
                return readFile(txtFile);
            }
        } catch (Exception e) {
            Log.e("DownloadedFragment", "Error reading lyrics file: " + e.getMessage());
        }
        return null;
    }

    private String readFile(File file) {
        try {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e("DownloadedFragment", "Error reading file: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onSongClick(List<LocalSong> songList, int position) {
        // Gửi intent đến Service để phát nhạc
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_PLAY);
        intent.putExtra("songList", new ArrayList<>(convertLocalSongsToSongs(songList)));
        intent.putExtra("position", position);
        requireContext().startService(intent);

        // Hiển thị player fragment
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showPlayer(convertLocalSongsToSongs(songList), position, "Nhạc đã tải");
        }
    }

    private List<Song> convertLocalSongsToSongs(List<LocalSong> localSongs) {
        Context context;
        try {
            context = requireContext();
        } catch (IllegalStateException e) {
            Log.e("DownloadedFragment", "Fragment not attached to activity", e);
            return new ArrayList<>();
        }
        
        List<Song> convertedSongs = new ArrayList<>();
        for (LocalSong localSong : localSongs) {
            Song song = new Song();
            song.setTitle(localSong.getTitle());
            song.setArtistId(localSong.getArtist());
            
            // Chuyển đổi Uri thành String và đảm bảo nó là URI hợp lệ
            Uri uri = localSong.getUri();
            if (uri != null) {
                String audioUrl = uri.toString();
                Log.d("DownloadedFragment", "Audio URL for " + localSong.getTitle() + ": " + audioUrl);
                song.setAudioUrl(audioUrl);
            } else {
                Log.e("DownloadedFragment", "URI is null for song: " + localSong.getTitle());
                continue; // Skip this song if URI is null
            }
            
            song.setDuration((int) localSong.getDuration());
            
            // Xử lý cover art
            String coverUrl = localSong.getCoverUrl();
            if (coverUrl != null && !coverUrl.isEmpty()) {
                Log.d("DownloadedFragment", "Cover URL for " + localSong.getTitle() + ": " + coverUrl);
                song.setCoverUrl(coverUrl);
            } else {
                song.setCoverUrl("android.resource://" + context.getPackageName() + "/mipmap/ic_launcher_new");
            }
            
            // Xử lý lyrics
            String lyrics = localSong.getLyrics();
            if (lyrics == null || lyrics.isEmpty()) {
                lyrics = "Chưa có lời bài hát";
            }
            song.setLyrics(lyrics);
            
            convertedSongs.add(song);
        }
        
        return convertedSongs;
    }

    private void playRandomSong() {
        if (localSongs.isEmpty()) {
            Toast.makeText(getContext(), "Không có bài hát nào để phát", Toast.LENGTH_SHORT).show();
            return;
        }
        int idx = new Random().nextInt(localSongs.size());
        onSongClick(localSongs, idx);
    }

    @Override
    public void onDestroy() {
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
        super.onDestroy();
    }
}