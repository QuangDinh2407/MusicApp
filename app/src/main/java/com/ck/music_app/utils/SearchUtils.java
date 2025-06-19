package com.ck.music_app.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class SearchUtils {

    /**
     * Chuẩn hóa chuỗi để tìm kiếm fuzzy:
     * - Loại bỏ dấu tiếng Việt
     * - Chuyển về chữ thường
     * - Loại bỏ khoảng trắng thừa
     */
    public static String normalizeString(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Loại bỏ dấu tiếng Việt
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Chuyển về chữ thường và loại bỏ khoảng trắng thừa
        normalized = normalized.toLowerCase().trim().replaceAll("\\s+", " ");

        return normalized;
    }

    /**
     * Tính toán độ tương đồng giữa hai chuỗi (Levenshtein Distance)
     */
    public static int calculateLevenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(
                                    dp[i - 1][j] + 1, // deletion
                                    dp[i][j - 1] + 1 // insertion
                            ),
                            dp[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1) // substitution
                    );
                }
            }
        }
        return dp[str1.length()][str2.length()];
    }

    /**
     * Tính toán điểm số tương đồng (0-100)
     */
    public static double calculateSimilarityScore(String query, String target) {
        if (query == null || target == null)
            return 0;
        if (query.isEmpty() || target.isEmpty())
            return 0;

        String normalizedQuery = normalizeString(query);
        String normalizedTarget = normalizeString(target);

        // Nếu chứa đầy đủ từ khóa -> điểm cao
        if (normalizedTarget.contains(normalizedQuery)) {
            // Điểm cao hơn nếu match từ đầu
            if (normalizedTarget.startsWith(normalizedQuery)) {
                return 95.0;
            }
            return 85.0;
        }

        // Tính điểm dựa trên Levenshtein Distance
        int distance = calculateLevenshteinDistance(normalizedQuery, normalizedTarget);
        int maxLength = Math.max(normalizedQuery.length(), normalizedTarget.length());

        if (maxLength == 0)
            return 100.0;

        double similarity = (1.0 - (double) distance / maxLength) * 100;

        // Threshold để loại bỏ kết quả quá khác biệt
        return similarity >= 40.0 ? similarity : 0.0;
    }

    /**
     * Kiểm tra xem có khớp fuzzy không
     */
    public static boolean isFuzzyMatch(String query, String target, double threshold) {
        return calculateSimilarityScore(query, target) >= threshold;
    }

    /**
     * Tách từ khóa thành các từ riêng biệt để tìm kiếm
     */
    public static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return tokens;
        }

        String normalized = normalizeString(input);
        String[] words = normalized.split("\\s+");

        for (String word : words) {
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }

        return tokens;
    }

    /**
     * Kiểm tra xem có chứa tất cả các từ khóa không
     */
    public static boolean containsAllTokens(String target, List<String> queryTokens) {
        if (queryTokens.isEmpty())
            return true;
        if (target == null || target.isEmpty())
            return false;

        String normalizedTarget = normalizeString(target);

        for (String token : queryTokens) {
            if (!normalizedTarget.contains(token)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Highlight các từ khóa trong kết quả
     */
    public static String highlightMatches(String text, String query) {
        if (text == null || query == null || query.isEmpty()) {
            return text;
        }

        String normalizedQuery = normalizeString(query);
        String normalizedText = normalizeString(text);

        // Tìm vị trí match và highlight
        int index = normalizedText.indexOf(normalizedQuery);
        if (index != -1) {
            // Tính vị trí tương ứng trong text gốc
            return text.substring(0, index) +
                    "<b>" + text.substring(index, index + normalizedQuery.length()) + "</b>" +
                    text.substring(index + normalizedQuery.length());
        }

        return text;
    }
}