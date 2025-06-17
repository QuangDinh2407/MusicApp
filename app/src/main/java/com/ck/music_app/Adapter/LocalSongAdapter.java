package com.ck.music_app.Adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.LocalSong;
import com.ck.music_app.R;
import com.ck.music_app.utils.MusicUtils;

import java.io.File;
import java.util.List;

public class LocalSongAdapter extends RecyclerView.Adapter<LocalSongAdapter.ViewHolder> {
    private static final String TAG = "LocalSongAdapter";
    private List<LocalSong> songList;
    private OnSongClickListener clickListener;
    private Context context;

    public interface OnSongClickListener {
        void onSongClick(List<LocalSong> songList, int position);
    }

    public LocalSongAdapter(Context context, List<LocalSong> songList, OnSongClickListener listener) {
        this.context = context;
        this.songList = songList;
        this.clickListener = listener;
        System.out.println("=== LocalSongAdapter Constructor ===");
        System.out.println("SongList size: " + (songList != null ? songList.size() : "null"));
        if (songList != null && !songList.isEmpty()) {
            System.out.println("First song: " + songList.get(0).getTitle());
            System.out.println("Last song: " + songList.get(songList.size() - 1).getTitle());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_local_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        System.out.println("onBindViewHolder position: " + position);
        LocalSong song = songList.get(position);
        System.out.println("Binding song: " + song.getTitle());

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvDuration.setText(formatDuration(song.getDuration()));

        // Load album art
        String coverUrl = song.getCoverUrl();

        Glide.with(context)
                .load(coverUrl)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onSongClick(songList, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            showSongOptionsMenu(v, song, position);
            return true;
        });
    }

    private void showSongOptionsMenu(View view, LocalSong song, int position) {
        PopupMenu popup = new PopupMenu(context, view);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Đổi tên bài hát");

        final EditText input = new EditText(context);
        input.setText(song.getTitle());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newTitle = input.getText().toString();
            if (!newTitle.isEmpty() && !newTitle.equals(song.getTitle())) {
                renameSong(song, newTitle, position);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteConfirmationDialog(LocalSong song, int position) {
        new AlertDialog.Builder(context)
            .setTitle("Xóa bài hát")
            .setMessage("Bạn có chắc muốn xóa bài hát này?")
            .setPositiveButton("Xóa", (dialog, which) -> deleteSong(song, position))
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void renameSong(LocalSong song, String newTitle, int position) {
        try {
            // Lấy đường dẫn file gốc từ URI
            String filePath = getRealPathFromUri(song.getUri());
            if (filePath == null) {
                Toast.makeText(context, "Không thể truy cập file", Toast.LENGTH_SHORT).show();
                return;
            }

            File oldFile = new File(filePath);
            if (!oldFile.exists()) {
                Toast.makeText(context, "File không tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }

            File parentDir = oldFile.getParentFile();
            if (parentDir == null) {
                Toast.makeText(context, "Không thể xác định thư mục", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo tên file mới (giữ nguyên phần mở rộng)
            String extension = filePath.substring(filePath.lastIndexOf("."));
            String normalizedTitle = MusicUtils.removeVietnameseDiacritics(newTitle);
            String newFileName = normalizedTitle + extension;
            File newFile = new File(parentDir, newFileName);

            // Đổi tên các file liên quan
            boolean success = true;
            
            // 1. Đổi tên file nhạc
            if (!oldFile.renameTo(newFile)) {
                success = false;
            }

            // 2. Đổi tên file ảnh nếu có
            String oldBaseName = oldFile.getName().substring(0, oldFile.getName().lastIndexOf("."));
            File oldCoverFile = new File(parentDir, oldBaseName + ".jpg");
            if (oldCoverFile.exists()) {
                File newCoverFile = new File(parentDir, normalizedTitle + ".jpg");
                if (!oldCoverFile.renameTo(newCoverFile)) {
                    success = false;
                }
                // Cập nhật coverUrl
                song.setCoverUrl(Uri.fromFile(newCoverFile).toString());
            }

            // 3. Đổi tên file lyrics nếu có
            File oldLyricsFile = new File(parentDir, oldBaseName + ".txt");
            if (oldLyricsFile.exists()) {
                File newLyricsFile = new File(parentDir, normalizedTitle + ".txt");
                if (!oldLyricsFile.renameTo(newLyricsFile)) {
                    success = false;
                }
            }

            if (success) {
                // Cập nhật MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.TITLE, newTitle);
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, newFileName);
                context.getContentResolver().update(song.getUri(), values, null, null);

                // Cập nhật danh sách
                song.setTitle(newTitle);
                notifyItemChanged(position);
                Toast.makeText(context, "Đã đổi tên bài hát và các file liên quan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Có lỗi khi đổi tên một số file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error renaming files: " + e.getMessage());
            Toast.makeText(context, "Lỗi khi đổi tên file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSong(LocalSong song, int position) {
        try {
            // Lấy đường dẫn file từ URI
            String filePath = getRealPathFromUri(song.getUri());
            if (filePath == null) {
                Toast.makeText(context, "Không thể truy cập file", Toast.LENGTH_SHORT).show();
                return;
            }

            File musicFile = new File(filePath);
            if (!musicFile.exists()) {
                Toast.makeText(context, "File không tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }

            File parentDir = musicFile.getParentFile();
            if (parentDir == null) {
                Toast.makeText(context, "Không thể xác định thư mục", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = true;

            // 1. Xóa file nhạc
            if (!musicFile.delete()) {
                success = false;
            }

            // 2. Xóa file ảnh nếu có
            String baseName = musicFile.getName().substring(0, musicFile.getName().lastIndexOf("."));
            File coverFile = new File(parentDir, baseName + ".jpg");
            if (coverFile.exists() && !coverFile.delete()) {
                success = false;
            }

            // 3. Xóa file lyrics nếu có
            File lyricsFile = new File(parentDir, baseName + ".txt");
            if (lyricsFile.exists() && !lyricsFile.delete()) {
                success = false;
            }

            // 4. Kiểm tra xem thư mục artist có còn file nào không
            File[] remainingFiles = parentDir.listFiles();
            if (remainingFiles != null && remainingFiles.length == 0) {
                // Nếu không còn file nào, xóa thư mục artist
                parentDir.delete();
            }

            if (success) {
                // Xóa khỏi MediaStore
                context.getContentResolver().delete(song.getUri(), null, null);

                // Xóa khỏi danh sách
                songList.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "Đã xóa bài hát và các file liên quan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Có lỗi khi xóa một số file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting files: " + e.getMessage());
            Toast.makeText(context, "Lỗi khi xóa file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getRealPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Audio.Media.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting real path: " + e.getMessage());
        }
        return null;
    }

    private String formatDuration(long durationMs) {
        int totalSec = (int) (durationMs / 1000);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvArtist, tvDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgMusicIcon);
            tvTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvSongArtist);
            tvDuration = itemView.findViewById(R.id.tvSongDuration);
        }
    }
} 