package com.ck.music_app.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import android.webkit.MimeTypeMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class DownloadUtils {
    private static final String TAG = "DownloadUtils";
    private static final String DEFAULT_ARTIST = "Unknown_Artist";

    public interface DownloadCallback {
        void onProgressUpdate(int progress);
        void onDownloadComplete(String filePath);
        void onError(String message);
    }

    public static String normalizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return DEFAULT_ARTIST;
        }
        // Chỉ thay thế các ký tự đặc biệt không hợp lệ trong tên file
        String temp = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // Xử lý khoảng trắng (tùy chọn, có thể bỏ nếu muốn giữ nguyên khoảng trắng)
        temp = temp.replaceAll("\\s+", "_");
        
        // Xử lý các trường hợp đặc biệt
        temp = temp.replaceAll("__+", "_"); // Thay nhiều dấu _ liên tiếp bằng một dấu _
        temp = temp.replaceAll("^_", ""); // Xóa dấu _ ở đầu
        temp = temp.replaceAll("_$", ""); // Xóa dấu _ ở cuối
        
        return temp;
    }

    public static void downloadSong(Context context, String songUrl, String title, String artist, 
                                  String coverUrl, String lyrics, DownloadCallback callback) {
        new Thread(() -> {
            try {
                // Validate parameters
                if (context == null) {
                    throw new IllegalArgumentException("Context cannot be null");
                }
                if (songUrl == null || songUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException("Song URL cannot be null or empty");
                }
                if (title == null || title.trim().isEmpty()) {
                    throw new IllegalArgumentException("Title cannot be null or empty");
                }

                // Normalize file names
                String normalizedTitle = normalizeFileName(title);
                String normalizedArtist = normalizeFileName(artist);
                
                // Use default artist if not provided
                if (normalizedArtist == null || normalizedArtist.trim().isEmpty()) {
                    Log.w(TAG, "Artist name not provided, using default: " + DEFAULT_ARTIST);
                    normalizedArtist = DEFAULT_ARTIST;
                }
                
                String baseFileName = normalizedTitle;
                
                // 1. Download audio file
                String audioFileName = baseFileName + ".mp3";
                Uri audioUri = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10 and above, use MediaStore
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Media.DISPLAY_NAME, audioFileName);
                    values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg");
                    // Tạo đường dẫn với tên nghệ sĩ
                    String relativePath = Environment.DIRECTORY_MUSIC + "/" + normalizedArtist;
                    values.put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath);
                    values.put(MediaStore.Audio.Media.IS_MUSIC, 1);
                    values.put(MediaStore.Audio.Media.TITLE, title);
                    values.put(MediaStore.Audio.Media.ARTIST, artist != null ? artist : DEFAULT_ARTIST);

                    audioUri = context.getContentResolver().insert(
                            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), 
                            values);

                    if (audioUri != null) {
                        try {
                            URL url = new URL(songUrl);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.connect();
                            
                            int fileLength = connection.getContentLength();
                            
                            try (OutputStream os = context.getContentResolver().openOutputStream(audioUri);
                                 InputStream is = connection.getInputStream()) {
                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                long totalBytes = 0;
                                
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                    totalBytes += bytesRead;
                                    
                                    if (fileLength > 0) {
                                        int progress = (int) ((totalBytes * 100) / fileLength);
                                        callback.onProgressUpdate(progress);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw e;
                        }
                    }
                } else {
                    // For older Android versions
                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    // Tạo thư mục artist nếu chưa tồn tại
                    File artistDir = new File(musicDir, normalizedArtist);
                    if (!artistDir.exists()) {
                        artistDir.mkdirs();
                    }
                    File file = new File(artistDir, audioFileName);
                    
                    URL url = new URL(songUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    
                    int fileLength = connection.getContentLength();
                    
                    try (FileOutputStream fos = new FileOutputStream(file);
                         InputStream is = connection.getInputStream()) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalBytes = 0;
                        
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalBytes += bytesRead;
                            
                            if (fileLength > 0) {
                                int progress = (int) ((totalBytes * 100) / fileLength);
                                callback.onProgressUpdate(progress);
                            }
                        }
                        audioUri = Uri.fromFile(file);
                    }
                }

                // 2. Download and save cover image
                if (coverUrl != null && !coverUrl.startsWith("android.resource")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // For Android 10 and above, use MediaStore.Images with Pictures directory
                        ContentValues imageValues = new ContentValues();
                        imageValues.put(MediaStore.Images.Media.DISPLAY_NAME, baseFileName + ".jpg");
                        imageValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        // Save in Pictures/MusicApp/artist_name/
                        String relativePath = Environment.DIRECTORY_PICTURES + "/MusicApp/" + normalizedArtist;
                        imageValues.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
                        // Add description to help identify the image
                        imageValues.put(MediaStore.Images.Media.DESCRIPTION, "Album art for " + title);

                        Uri imageUri = context.getContentResolver().insert(
                                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                                imageValues);

                        if (imageUri != null) {
                            try (OutputStream os = context.getContentResolver().openOutputStream(imageUri);
                                 InputStream is = new URL(coverUrl).openStream()) {
                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    } else {
                        // For older Android versions
                        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                        File artistDir = new File(musicDir, normalizedArtist);
                        if (!artistDir.exists()) {
                            artistDir.mkdirs();
                        }
                        // Save album art in the artist directory
                        File coverFile = new File(artistDir, baseFileName + ".jpg");
                        try (FileOutputStream fos = new FileOutputStream(coverFile);
                             InputStream is = new URL(coverUrl).openStream()) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }

                // 3. Save lyrics
                if (lyrics != null && !lyrics.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // For Android 10 and above, save lyrics in Download directory
                        ContentValues textValues = new ContentValues();
                        textValues.put(MediaStore.Downloads.DISPLAY_NAME, baseFileName + ".txt");
                        textValues.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                        // Save in Download/MusicApp/artist_name/
                        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/MusicApp/" + normalizedArtist;
                        textValues.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);

                        Uri textUri = context.getContentResolver().insert(
                                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                                textValues);

                        if (textUri != null) {
                            try (OutputStream os = context.getContentResolver().openOutputStream(textUri)) {
                                os.write(lyrics.getBytes());
                            }
                        }
                    } else {
                        // For older Android versions
                        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                        File artistDir = new File(musicDir, normalizedArtist);
                        if (!artistDir.exists()) {
                            artistDir.mkdirs();
                        }
                        // Save lyrics file in the artist directory
                        File lyricsFile = new File(artistDir, baseFileName + ".txt");
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(lyricsFile))) {
                            writer.write(lyrics);
                        }
                    }
                }

                callback.onProgressUpdate(100);
                callback.onDownloadComplete(audioUri.toString());

            } catch (IOException e) {
                Log.e(TAG, "Error downloading song: " + e.getMessage());
                callback.onError("Lỗi tải xuống: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid parameters: " + e.getMessage());
                callback.onError("Lỗi tham số: " + e.getMessage());
            }
        }).start();
    }
} 