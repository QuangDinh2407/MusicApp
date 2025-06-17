package com.ck.music_app.utils;

import java.util.concurrent.TimeUnit;

public class MusicUtils {
    public static String formatTime(long milliseconds) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }

    /**
     * Loại bỏ dấu tiếng Việt khỏi chuỗi (normalize to ASCII, remove diacritics)
     */
    public static String removeVietnameseDiacritics(String input) {
        if (input == null) return null;
        String temp = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Xử lý riêng cho đ/Đ
        temp = temp.replaceAll("đ", "d").replaceAll("Đ", "D");
        return temp;
    }
} 