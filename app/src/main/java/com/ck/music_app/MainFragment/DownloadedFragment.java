package com.ck.music_app.MainFragment;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.ck.music_app.R;
import com.ck.music_app.utils.MusicUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DownloadedFragment extends Fragment {
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private RecyclerView rvLocalSongs;
    private LocalSongAdapter adapter;
    private List<LocalSong> localSongs = new ArrayList<>();
    private MediaPlayer mediaPlayer;

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
        
        adapter = new LocalSongAdapter(localSongs);
        rvLocalSongs.setAdapter(adapter);

        // Kiểm tra và yêu cầu quyền
        checkAndRequestPermissions();

        // Nút back
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            // Xóa fragment khỏi back stack
            requireActivity().getSupportFragmentManager().popBackStack();
            
            // Ẩn fragment container và hiện lại ViewPager2
            View fragmentContainer = requireActivity().findViewById(R.id.fragment_container);
            View viewPager = requireActivity().findViewById(R.id.view_pager);
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
            if (viewPager != null) viewPager.setVisibility(View.VISIBLE);
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
                MediaStore.Audio.Media.DURATION
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

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String artist = cursor.getString(artistCol);
                long duration = cursor.getLong(durationCol);
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                Log.d("DownloadedFragment", "Found song: " + title + " - " + artist + " - " + uri.toString());
                localSongs.add(new LocalSong(title, artist, duration, uri));
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

        adapter.notifyDataSetChanged();
    }

    private void playRandomSong() {
        if (localSongs.isEmpty()) {
            Toast.makeText(getContext(), "Không có bài hát nào để phát", Toast.LENGTH_SHORT).show();
            return;
        }
        int idx = new Random().nextInt(localSongs.size());
        playSong(localSongs.get(idx));
    }

    private void playSong(LocalSong song) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getContext(), song.uri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(getContext(), "Đang phát: " + song.title, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("DownloadedFragment", "Error playing song: " + e.getMessage());
            Toast.makeText(getContext(), "Lỗi khi phát bài hát", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    // Model bài hát local
    static class LocalSong {
        String title, artist;
        long duration;
        Uri uri;
        LocalSong(String title, String artist, long duration, Uri uri) {
            this.title = title;
            this.artist = artist;
            this.duration = duration;
            this.uri = uri;
        }
    }

    // Adapter cho RecyclerView
    class LocalSongAdapter extends RecyclerView.Adapter<LocalSongAdapter.LocalSongViewHolder> {
        List<LocalSong> songs;
        LocalSongAdapter(List<LocalSong> songs) { this.songs = songs; }
        
        @NonNull
        @Override
        public LocalSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_local_song, parent, false);
            return new LocalSongViewHolder(v);
        }
        
        @Override
        public void onBindViewHolder(@NonNull LocalSongViewHolder holder, int position) {
            LocalSong song = songs.get(position);
            holder.tvSongTitle.setText(song.title);
            holder.tvSongArtist.setText(song.artist);
            holder.tvSongDuration.setText(formatDuration(song.duration));
            
            // Xử lý click để phát nhạc
            holder.itemView.setOnClickListener(v -> playSong(song));
            
            // Xử lý long click để hiển thị menu
            holder.itemView.setOnLongClickListener(v -> {
                showSongOptionsMenu(v, song, position);
                return true;
            });
        }
        
        private void showSongOptionsMenu(View view, LocalSong song, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.menu_local_song, popup.getMenu());
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_rename) {
                    showRenameDialog(song, position);
                    return true;
                } else if (itemId == R.id.menu_delete) {
                    showDeleteConfirmationDialog(song, position);
                    return true;
                }
                return false;
            });
            
            popup.show();
        }
        
        private void showRenameDialog(LocalSong song, int position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Đổi tên bài hát");
            
            final EditText input = new EditText(requireContext());
            input.setText(song.title);
            builder.setView(input);
            
            builder.setPositiveButton("OK", (dialog, which) -> {
                String newTitle = input.getText().toString();
                if (!newTitle.isEmpty() && !newTitle.equals(song.title)) {
                    renameSong(song, newTitle, position);
                }
            });
            builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
            
            builder.show();
        }
        
        private void showDeleteConfirmationDialog(LocalSong song, int position) {
            new AlertDialog.Builder(requireContext())
                .setTitle("Xóa bài hát")
                .setMessage("Bạn có chắc muốn xóa bài hát này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteSong(song, position))
                .setNegativeButton("Hủy", null)
                .show();
        }
        
        private void renameSong(LocalSong song, String newTitle, int position) {
            try {
                // Lấy đường dẫn file gốc từ URI
                String filePath = getRealPathFromUri(song.uri);
                if (filePath == null) {
                    Toast.makeText(requireContext(), "Không thể truy cập file", Toast.LENGTH_SHORT).show();
                    return;
                }

                File oldFile = new File(filePath);
                if (!oldFile.exists()) {
                    Toast.makeText(requireContext(), "File không tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Tạo tên file mới (giữ nguyên phần mở rộng)
                String extension = filePath.substring(filePath.lastIndexOf("."));
                String newFileName = MusicUtils.removeVietnameseDiacritics(newTitle) + extension;
                File newFile = new File(oldFile.getParent(), newFileName);

                // Đổi tên file
                if (oldFile.renameTo(newFile)) {
                    // Cập nhật MediaStore
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Media.TITLE, newTitle);
                    values.put(MediaStore.Audio.Media.DISPLAY_NAME, newFileName);
                    requireContext().getContentResolver().update(song.uri, values, null, null);

                    // Cập nhật danh sách
                    song.title = newTitle;
                    adapter.notifyItemChanged(position);
                    Toast.makeText(requireContext(), "Đã đổi tên bài hát", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Không thể đổi tên file", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("DownloadedFragment", "Error renaming file: " + e.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi đổi tên file", Toast.LENGTH_SHORT).show();
            }
        }

        private void deleteSong(LocalSong song, int position) {
            try {
                // Lấy đường dẫn file từ URI
                String filePath = getRealPathFromUri(song.uri);
                if (filePath == null) {
                    Toast.makeText(requireContext(), "Không thể truy cập file", Toast.LENGTH_SHORT).show();
                    return;
                }

                File file = new File(filePath);
                if (!file.exists()) {
                    Toast.makeText(requireContext(), "File không tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Xóa file
                if (file.delete()) {
                    // Xóa khỏi MediaStore
                    requireContext().getContentResolver().delete(song.uri, null, null);

                    // Xóa khỏi danh sách
                    songs.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(requireContext(), "Đã xóa bài hát", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Không thể xóa file", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("DownloadedFragment", "Error deleting file: " + e.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi xóa file", Toast.LENGTH_SHORT).show();
            }
        }

        private String getRealPathFromUri(Uri uri) {
            String[] projection = {MediaStore.Audio.Media.DATA};
            try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                Log.e("DownloadedFragment", "Error getting real path: " + e.getMessage());
            }
            return null;
        }
        
        @Override
        public int getItemCount() { return songs.size(); }
        
        class LocalSongViewHolder extends RecyclerView.ViewHolder {
            TextView tvSongTitle, tvSongArtist, tvSongDuration;
            LocalSongViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
                tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
                tvSongDuration = itemView.findViewById(R.id.tvSongDuration);
            }
        }
    }

    private String formatDuration(long durationMs) {
        int totalSec = (int) (durationMs / 1000);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }
}
//```
//
//        ### Các thay đổi chính
//1. **Xử lý quyền**:
//        - Thêm `checkAndRequestPermissions()` để kiểm tra và yêu cầu quyền trước khi tải nhạc.
//   - Xử lý kết quả quyền trong `onRequestPermissionsResult` để tải nhạc nếu quyền được cấp hoặc hiển thị thông báo nếu bị từ chối.
//        - Hiển thị Toast để thông báo trạng thái quyền.
//
//2. **Truy vấn MediaStore**:
//        - Sử dụng `MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)` cho Android 10+.
//        - Thêm điều kiện `DURATION >= 30000` để lọc các file âm thanh ngắn (như nhạc chuông).
//        - Sắp xếp theo tiêu đề (`TITLE ASC`) để danh sách dễ đọc hơn.
//        - Bỏ cột `DATA` vì không cần thiết.
//
//        3. **Gỡ lỗi**:
//        - Thêm log chi tiết về số lượng bài hát, lỗi truy vấn, và trạng thái context.
//        - Hiển thị Toast khi không tìm thấy bài hát hoặc gặp lỗi.
//
//        4. **Sửa lỗi giao diện**:
//        - Sửa `TextView btnPlayRandom` thành `Button btnPlayRandom` để khớp với layout XML (nút `btnPlayRandom` là một `Button` trong `fragment_downloaded.xml`).
//        - Thêm Toast khi phát nhạc để xác nhận bài hát đang phát.
//
//        5. **Xử lý lỗi phát nhạc**:
//        - Thêm xử lý ngoại lệ và thông báo khi phát nhạc thất bại.
//
//### Các bước kiểm tra và khắc phục
//1. **Kiểm tra quyền trong ứng dụng**:
//        - Chạy ứng dụng và đảm bảo bạn cấp quyền `READ_MEDIA_AUDIO` (Android 13+) hoặc `READ_EXTERNAL_STORAGE` (Android 12 trở xuống).
//        - Nếu quyền bị từ chối, ứng dụng sẽ hiển thị Toast yêu cầu cấp quyền.
//
//        2. **Kiểm tra file nhạc trên thiết bị**:
//        - Đảm bảo thiết bị có file nhạc (định dạng MP3, AAC, v.v.) trong bộ nhớ trong hoặc thẻ SD.
//        - Sử dụng ứng dụng quản lý file để xác nhận các file nhạc nằm ở đâu (ví dụ: thư mục `Music` hoặc `Downloads`).
//        - Nếu thiết bị không có nhạc, MediaStore sẽ trả về danh sách rỗng.
//
//        3. **Kiểm tra log**:
//        - Mở Logcat trong Android Studio, lọc theo tag `DownloadedFragment`.
//        - Xem các log để biết:
//        - Số lượng bài hát tìm thấy (`Total songs found: ...`).
//        - Chi tiết mỗi bài hát (`Found song: ...`).
//        - Lỗi nếu có (`Error querying MediaStore: ...`).
//
//        4. **Kiểm tra layout XML**:
//        - Đảm bảo file `fragment_downloaded.xml` khớp với mã Java, đặc biệt là ID của `rvLocalSongs`, `btnBack`, `btnPlayRandom`, và `btnRescan`.
//        - Kiểm tra xem `btnPlayRandom` có phải là `Button` trong XML không.
//
//5. **Kiểm tra trên các phiên bản Android**:
//        - Nếu bạn đang thử nghiệm trên Android 10+, hãy đảm bảo ứng dụng được cấp quyền truy cập bộ nhớ.
//        - Trên Android 13, quyền `READ_MEDIA_AUDIO` là bắt buộc.
//
//6. **Thêm file nhạc nếu cần**:
//        - Nếu thiết bị không có nhạc, tải một vài file MP3 vào thư mục `Music` hoặc `Downloads` và chạy lại nút "Quét lại" (`btnRescan`).
//
//        ### Thêm khuyến nghị
//- **Hiển thị giao diện khi không có nhạc**: Nếu `localSongs` rỗng, bạn có thể hiển thị một `TextView` hoặc hình ảnh thông báo "Không tìm thấy bài hát" thay vì để RecyclerView trống.
//- **Cải thiện hiệu suất**: Nếu danh sách nhạc lớn, cân nhắc tải dữ liệu trong luồng nền (ví dụ: dùng `AsyncTask` hoặc `Coroutine` trong Kotlin).
//        - **Hỗ trợ bộ nhớ ngoài**: Nếu thiết bị có thẻ SD, đảm bảo ứng dụng có quyền truy cập thẻ SD qua cài đặt hệ thống.
//
//        ### Nếu vẫn không hoạt động
//Nếu sau khi áp dụng các thay đổi trên mà vẫn không lấy được nhạc, hãy cung cấp thêm thông tin:
//        - Phiên bản Android của thiết bị thử nghiệm.
//        - Nội dung Logcat (lọc tag `DownloadedFragment`).
//        - Vị trí lưu file nhạc trên thiết bị (ví dụ: `/storage/emulated/0/Music`).
//        - Bất kỳ thông báo lỗi nào từ Toast hoặc Logcat.
//
//Tôi sẽ hỗ trợ bạn tiếp tục khắc phục!