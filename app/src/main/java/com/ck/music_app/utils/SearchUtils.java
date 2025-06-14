package com.ck.music_app.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class SearchUtils {

    /**
     * Loại bỏ dấu tiếng Việt và chuyển về chữ thường
     */
    public static String removeAccents(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Normalize và loại bỏ dấu
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Xử lý các ký tự đặc biệt tiếng Việt
        withoutAccents = withoutAccents.replace("đ", "d").replace("Đ", "D");

        return withoutAccents.toLowerCase().trim();
    }

    /**
     * Tính độ tương đồng giữa 2 chuỗi (Levenshtein Distance)
     */
    public static int calculateLevenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }

        s1 = removeAccents(s1);
        s2 = removeAccents(s2);

        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[len1][len2];
    }

    /**
     * Kiểm tra xem query có match với text không (fuzzy search)
     */
    public static boolean isFuzzyMatch(String query, String text, double threshold) {
        if (query == null || text == null) {
            return false;
        }

        String normalizedQuery = removeAccents(query);
        String normalizedText = removeAccents(text);

        // Exact match
        if (normalizedText.contains(normalizedQuery)) {
            return true;
        }

        // Fuzzy match với threshold
        int distance = calculateLevenshteinDistance(normalizedQuery, normalizedText);
        int maxLength = Math.max(normalizedQuery.length(), normalizedText.length());

        if (maxLength == 0)
            return true;

        double similarity = 1.0 - (double) distance / maxLength;
        return similarity >= threshold;
    }

    /**
     * Tách query thành các từ khóa
     */
    public static List<String> splitQuery(String query) {
        List<String> keywords = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return keywords;
        }

        String[] words = query.trim().split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                keywords.add(removeAccents(word));
            }
        }

        return keywords;
    }

    /**
     * Tính điểm relevance cho kết quả tìm kiếm
     */
    public static double calculateRelevanceScore(String query, String title, String subtitle) {
        if (query == null || query.trim().isEmpty()) {
            return 0.0;
        }

        String normalizedQuery = removeAccents(query);
        String normalizedTitle = removeAccents(title != null ? title : "");
        String normalizedSubtitle = removeAccents(subtitle != null ? subtitle : "");

        double score = 0.0;

        // Exact match trong title có điểm cao nhất
        if (normalizedTitle.contains(normalizedQuery)) {
            score += 100.0;
        }

        // Exact match trong subtitle
        if (normalizedSubtitle.contains(normalizedQuery)) {
            score += 50.0;
        }

        // Fuzzy match trong title
        double titleSimilarity = 1.0 - (double) calculateLevenshteinDistance(normalizedQuery, normalizedTitle) /
                Math.max(normalizedQuery.length(), normalizedTitle.length());
        score += titleSimilarity * 80.0;

        // Fuzzy match trong subtitle
        double subtitleSimilarity = 1.0 - (double) calculateLevenshteinDistance(normalizedQuery, normalizedSubtitle) /
                Math.max(normalizedQuery.length(), normalizedSubtitle.length());
        score += subtitleSimilarity * 40.0;

        // Bonus cho match từ đầu
        if (normalizedTitle.startsWith(normalizedQuery)) {
            score += 20.0;
        }

        return score;
    }

    /**
     * Tạo gợi ý từ khóa
     */
    public static List<String> generateSuggestions(String query, List<String> allTexts) {
        List<String> suggestions = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return suggestions;
        }

        String normalizedQuery = removeAccents(query);

        for (String text : allTexts) {
            String normalizedText = removeAccents(text);

            // Gợi ý các từ bắt đầu bằng query
            if (normalizedText.startsWith(normalizedQuery) && !suggestions.contains(text)) {
                suggestions.add(text);
            }

            // Gợi ý các từ chứa query
            if (normalizedText.contains(normalizedQuery) && !suggestions.contains(text)) {
                suggestions.add(text);
            }

            // Giới hạn số lượng gợi ý
            if (suggestions.size() >= 10) {
                break;
            }
        }

        return suggestions;
    }
}