package com.ck.music_app.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ck.music_app.utils.SearchUtils;

public class EmotionGenreSearchHelper {

    // Emotion keywords mapping to genres/moods
    private static final Map<String, List<String>> EMOTION_KEYWORDS = new HashMap<>();
    private static final Map<String, List<String>> GENRE_KEYWORDS = new HashMap<>();
    private static final Map<String, List<String>> MOOD_KEYWORDS = new HashMap<>();

    static {
        // Happy emotions
        EMOTION_KEYWORDS.put("happy", Arrays.asList("pop", "dance", "electronic", "upbeat", "cheerful", "joyful"));
        EMOTION_KEYWORDS.put("vui", Arrays.asList("pop", "dance", "electronic", "upbeat", "cheerful", "joyful"));
        EMOTION_KEYWORDS.put("hạnh phúc", Arrays.asList("pop", "dance", "electronic", "upbeat", "cheerful", "joyful"));
        EMOTION_KEYWORDS.put("vui vẻ", Arrays.asList("pop", "dance", "electronic", "upbeat", "cheerful", "joyful"));

        // Sad emotions
        EMOTION_KEYWORDS.put("sad", Arrays.asList("ballad", "blues", "melancholy", "emotional", "slow"));
        EMOTION_KEYWORDS.put("buồn", Arrays.asList("ballad", "blues", "melancholy", "emotional", "slow"));
        EMOTION_KEYWORDS.put("tâm trạng", Arrays.asList("ballad", "blues", "melancholy", "emotional", "slow"));
        EMOTION_KEYWORDS.put("cô đơn", Arrays.asList("ballad", "blues", "melancholy", "emotional", "slow"));

        // Energetic emotions
        EMOTION_KEYWORDS.put("energetic", Arrays.asList("rock", "metal", "punk", "hardcore", "fast", "powerful"));
        EMOTION_KEYWORDS.put("năng lượng", Arrays.asList("rock", "metal", "punk", "hardcore", "fast", "powerful"));
        EMOTION_KEYWORDS.put("mạnh mẽ", Arrays.asList("rock", "metal", "punk", "hardcore", "fast", "powerful"));
        EMOTION_KEYWORDS.put("sôi động", Arrays.asList("rock", "metal", "punk", "hardcore", "fast", "powerful"));

        // Calm emotions
        EMOTION_KEYWORDS.put("calm", Arrays.asList("ambient", "classical", "instrumental", "peaceful", "relaxing"));
        EMOTION_KEYWORDS.put("bình yên", Arrays.asList("ambient", "classical", "instrumental", "peaceful", "relaxing"));
        EMOTION_KEYWORDS.put("thư giãn", Arrays.asList("ambient", "classical", "instrumental", "peaceful", "relaxing"));
        EMOTION_KEYWORDS.put("yên tĩnh", Arrays.asList("ambient", "classical", "instrumental", "peaceful", "relaxing"));

        // Romantic emotions
        EMOTION_KEYWORDS.put("romantic", Arrays.asList("love", "ballad", "soft", "tender", "sweet"));
        EMOTION_KEYWORDS.put("lãng mạn", Arrays.asList("love", "ballad", "soft", "tender", "sweet"));
        EMOTION_KEYWORDS.put("tình yêu", Arrays.asList("love", "ballad", "soft", "tender", "sweet"));
        EMOTION_KEYWORDS.put("yêu", Arrays.asList("love", "ballad", "soft", "tender", "sweet"));

        // Genre keywords
        GENRE_KEYWORDS.put("pop", Arrays.asList("pop", "mainstream", "chart", "hit", "popular"));
        GENRE_KEYWORDS.put("rock", Arrays.asList("rock", "guitar", "band", "alternative"));
        GENRE_KEYWORDS.put("jazz", Arrays.asList("jazz", "swing", "blues", "saxophone", "piano"));
        GENRE_KEYWORDS.put("classical", Arrays.asList("classical", "orchestra", "symphony", "piano", "violin"));
        GENRE_KEYWORDS.put("electronic", Arrays.asList("electronic", "edm", "techno", "house", "synth"));
        GENRE_KEYWORDS.put("hip hop", Arrays.asList("hip hop", "rap", "urban", "beats"));
        GENRE_KEYWORDS.put("country", Arrays.asList("country", "folk", "acoustic", "rural"));
        GENRE_KEYWORDS.put("r&b", Arrays.asList("r&b", "soul", "funk", "groove"));

        // Vietnamese genres
        GENRE_KEYWORDS.put("nhạc trẻ", Arrays.asList("pop", "young", "modern", "contemporary"));
        GENRE_KEYWORDS.put("bolero", Arrays.asList("bolero", "traditional", "classic", "vintage"));
        GENRE_KEYWORDS.put("dân ca", Arrays.asList("folk", "traditional", "cultural", "heritage"));
        GENRE_KEYWORDS.put("nhạc cách mạng", Arrays.asList("revolutionary", "patriotic", "historical"));

        // Mood keywords
        MOOD_KEYWORDS.put("workout", Arrays.asList("energetic", "fast", "motivational", "pump", "gym"));
        MOOD_KEYWORDS.put("tập thể dục", Arrays.asList("energetic", "fast", "motivational", "pump", "gym"));
        MOOD_KEYWORDS.put("study", Arrays.asList("focus", "concentration", "ambient", "instrumental", "calm"));
        MOOD_KEYWORDS.put("học tập", Arrays.asList("focus", "concentration", "ambient", "instrumental", "calm"));
        MOOD_KEYWORDS.put("sleep", Arrays.asList("lullaby", "soft", "peaceful", "quiet", "relaxing"));
        MOOD_KEYWORDS.put("ngủ", Arrays.asList("lullaby", "soft", "peaceful", "quiet", "relaxing"));
        MOOD_KEYWORDS.put("party", Arrays.asList("dance", "upbeat", "fun", "celebration", "energetic"));
        MOOD_KEYWORDS.put("tiệc tung", Arrays.asList("dance", "upbeat", "fun", "celebration", "energetic"));
        MOOD_KEYWORDS.put("driving", Arrays.asList("road", "journey", "adventure", "freedom"));
        MOOD_KEYWORDS.put("lái xe", Arrays.asList("road", "journey", "adventure", "freedom"));
    }

    /**
     * Detect if a search query contains emotion/genre/mood keywords
     */
    public static boolean containsEmotionOrGenre(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        String normalizedQuery = SearchUtils.removeAccents(query.toLowerCase());

        // Check emotion keywords
        for (String emotion : EMOTION_KEYWORDS.keySet()) {
            if (normalizedQuery.contains(SearchUtils.removeAccents(emotion))) {
                return true;
            }
        }

        // Check genre keywords
        for (String genre : GENRE_KEYWORDS.keySet()) {
            if (normalizedQuery.contains(SearchUtils.removeAccents(genre))) {
                return true;
            }
        }

        // Check mood keywords
        for (String mood : MOOD_KEYWORDS.keySet()) {
            if (normalizedQuery.contains(SearchUtils.removeAccents(mood))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract emotion/genre/mood keywords from search query
     */
    public static List<String> extractEmotionGenreKeywords(String query) {
        List<String> keywords = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return keywords;
        }

        String normalizedQuery = SearchUtils.removeAccents(query.toLowerCase());

        // Extract emotion keywords
        for (Map.Entry<String, List<String>> entry : EMOTION_KEYWORDS.entrySet()) {
            if (normalizedQuery.contains(SearchUtils.removeAccents(entry.getKey()))) {
                keywords.addAll(entry.getValue());
            }
        }

        // Extract genre keywords
        for (Map.Entry<String, List<String>> entry : GENRE_KEYWORDS.entrySet()) {
            if (normalizedQuery.contains(SearchUtils.removeAccents(entry.getKey()))) {
                keywords.addAll(entry.getValue());
            }
        }

        // Extract mood keywords
        for (Map.Entry<String, List<String>> entry : MOOD_KEYWORDS.entrySet()) {
            if (normalizedQuery.contains(SearchUtils.removeAccents(entry.getKey()))) {
                keywords.addAll(entry.getValue());
            }
        }

        return keywords;
    }

    /**
     * Get search suggestions based on emotions/genres
     */
    public static List<String> getEmotionGenreSuggestions(String query) {
        List<String> suggestions = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            // Return popular emotion/genre suggestions
            suggestions.addAll(Arrays.asList(
                    "nhạc vui", "nhạc buồn", "nhạc thư giãn", "nhạc lãng mạn",
                    "nhạc tập thể dục", "nhạc học tập", "nhạc ngủ", "nhạc tiệc tung",
                    "pop", "rock", "jazz", "classical", "electronic", "hip hop",
                    "ballad", "dance", "acoustic", "instrumental"));
        } else {
            String normalizedQuery = SearchUtils.removeAccents(query.toLowerCase());

            // Suggest related emotions/genres
            if (normalizedQuery.contains("vui") || normalizedQuery.contains("happy")) {
                suggestions.addAll(Arrays.asList("nhạc vui", "pop", "dance", "upbeat", "cheerful"));
            }
            if (normalizedQuery.contains("buon") || normalizedQuery.contains("sad")) {
                suggestions.addAll(Arrays.asList("nhạc buồn", "ballad", "blues", "melancholy"));
            }
            if (normalizedQuery.contains("thu gian") || normalizedQuery.contains("relax")) {
                suggestions.addAll(Arrays.asList("nhạc thư giãn", "ambient", "classical", "peaceful"));
            }
            if (normalizedQuery.contains("lang man") || normalizedQuery.contains("romantic")) {
                suggestions.addAll(Arrays.asList("nhạc lãng mạn", "love songs", "ballad", "soft"));
            }
            if (normalizedQuery.contains("tap the duc") || normalizedQuery.contains("workout")) {
                suggestions.addAll(Arrays.asList("nhạc tập thể dục", "energetic", "pump", "motivational"));
            }
        }

        // Limit suggestions
        if (suggestions.size() > 10) {
            suggestions = suggestions.subList(0, 10);
        }

        return suggestions;
    }

    /**
     * Calculate emotion/genre match score for a song
     */
    public static double calculateEmotionGenreScore(String query, String songTitle, String artistName, String genreId) {
        double score = 0.0;

        List<String> extractedKeywords = extractEmotionGenreKeywords(query);
        if (extractedKeywords.isEmpty()) {
            return score;
        }

        String normalizedTitle = SearchUtils.removeAccents(songTitle != null ? songTitle.toLowerCase() : "");
        String normalizedArtist = SearchUtils.removeAccents(artistName != null ? artistName.toLowerCase() : "");
        String normalizedGenre = SearchUtils.removeAccents(genreId != null ? genreId.toLowerCase() : "");

        // Check if extracted keywords match song metadata
        for (String keyword : extractedKeywords) {
            String normalizedKeyword = SearchUtils.removeAccents(keyword.toLowerCase());

            // Match in title
            if (normalizedTitle.contains(normalizedKeyword)) {
                score += 10.0;
            }

            // Match in artist name
            if (normalizedArtist.contains(normalizedKeyword)) {
                score += 5.0;
            }

            // Match in genre
            if (normalizedGenre.contains(normalizedKeyword)) {
                score += 15.0; // Genre match is most important
            }

            // Partial matches
            if (SearchUtils.isFuzzyMatch(normalizedKeyword, normalizedTitle, 0.7)) {
                score += 3.0;
            }
            if (SearchUtils.isFuzzyMatch(normalizedKeyword, normalizedGenre, 0.8)) {
                score += 8.0;
            }
        }

        return score;
    }

    /**
     * Get popular emotion/genre search terms
     */
    public static List<String> getPopularEmotionGenreTerms() {
        return Arrays.asList(
                "nhạc vui", "nhạc buồn", "nhạc thư giãn", "nhạc lãng mạn",
                "nhạc tập thể dục", "nhạc học tập", "nhạc ngủ", "nhạc tiệc tung",
                "pop", "rock", "jazz", "classical", "electronic", "hip hop",
                "ballad", "dance", "acoustic", "instrumental", "blues", "country",
                "happy music", "sad music", "relaxing music", "romantic music",
                "workout music", "study music", "sleep music", "party music");
    }

    /**
     * Check if query is specifically asking for emotion/genre search
     */
    public static boolean isEmotionGenreQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        String normalizedQuery = SearchUtils.removeAccents(query.toLowerCase());

        // Check for specific patterns
        return normalizedQuery.startsWith("nhac ") ||
                normalizedQuery.startsWith("music ") ||
                normalizedQuery.contains(" music") ||
                normalizedQuery.contains(" nhac") ||
                containsEmotionOrGenre(query);
    }
}