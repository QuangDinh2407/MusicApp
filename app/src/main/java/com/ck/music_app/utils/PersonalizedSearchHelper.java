package com.ck.music_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ck.music_app.Model.SearchResult;
import com.ck.music_app.Model.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonalizedSearchHelper {
    
    private static final String TAG = "PersonalizedSearchHelper";
    private static final String PREFS_NAME = "personalized_search_prefs";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final String KEY_PLAY_HISTORY = "play_history";
    private static final String KEY_GENRE_PREFERENCES = "genre_preferences";
    private static final String KEY_ARTIST_PREFERENCES = "artist_preferences";
    
    private Context context;
    private SharedPreferences prefs;
    private Gson gson;
    
    // User preferences data
    private Map<String, Integer> searchFrequency; // query -> count
    private Map<String, Integer> playHistory; // songId -> count
    private Map<String, Integer> genrePreferences; // genreId -> count
    private Map<String, Integer> artistPreferences; // artistId -> count
    
    public PersonalizedSearchHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        loadUserPreferences();
    }
    
    private void loadUserPreferences() {
        // Load search frequency
        String searchHistoryJson = prefs.getString(KEY_SEARCH_HISTORY, "{}");
        Type searchType = new TypeToken<Map<String, Integer>>(){}.getType();
        searchFrequency = gson.fromJson(searchHistoryJson, searchType);
        if (searchFrequency == null) {
            searchFrequency = new HashMap<>();
        }
        
        // Load play history
        String playHistoryJson = prefs.getString(KEY_PLAY_HISTORY, "{}");
        Type playType = new TypeToken<Map<String, Integer>>(){}.getType();
        playHistory = gson.fromJson(playHistoryJson, playType);
        if (playHistory == null) {
            playHistory = new HashMap<>();
        }
        
        // Load genre preferences
        String genrePrefsJson = prefs.getString(KEY_GENRE_PREFERENCES, "{}");
        Type genreType = new TypeToken<Map<String, Integer>>(){}.getType();
        genrePreferences = gson.fromJson(genrePrefsJson, genreType);
        if (genrePreferences == null) {
            genrePreferences = new HashMap<>();
        }
        
        // Load artist preferences
        String artistPrefsJson = prefs.getString(KEY_ARTIST_PREFERENCES, "{}");
        Type artistType = new TypeToken<Map<String, Integer>>(){}.getType();
        artistPreferences = gson.fromJson(artistPrefsJson, artistType);
        if (artistPreferences == null) {
            artistPreferences = new HashMap<>();
        }
        
        Log.d(TAG, "Loaded user preferences - Searches: " + searchFrequency.size() + 
                   ", Plays: " + playHistory.size() + 
                   ", Genres: " + genrePreferences.size() + 
                   ", Artists: " + artistPreferences.size());
    }
    
    private void saveUserPreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putString(KEY_SEARCH_HISTORY, gson.toJson(searchFrequency));
        editor.putString(KEY_PLAY_HISTORY, gson.toJson(playHistory));
        editor.putString(KEY_GENRE_PREFERENCES, gson.toJson(genrePreferences));
        editor.putString(KEY_ARTIST_PREFERENCES, gson.toJson(artistPreferences));
        
        editor.apply();
        Log.d(TAG, "Saved user preferences");
    }
    
    /**
     * Record a search query
     */
    public void recordSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        
        String normalizedQuery = SearchUtils.removeAccents(query.trim().toLowerCase());
        searchFrequency.put(normalizedQuery, searchFrequency.getOrDefault(normalizedQuery, 0) + 1);
        saveUserPreferences();
    }
    
    /**
     * Record a song play
     */
    public void recordSongPlay(Song song) {
        if (song == null) return;
        
        // Record song play
        playHistory.put(song.getSongId(), playHistory.getOrDefault(song.getSongId(), 0) + 1);
        
        // Record genre preference
        if (song.getGenreId() != null) {
            genrePreferences.put(song.getGenreId(), genrePreferences.getOrDefault(song.getGenreId(), 0) + 1);
        }
        
        // Record artist preference
        if (song.getArtistId() != null) {
            artistPreferences.put(song.getArtistId(), artistPreferences.getOrDefault(song.getArtistId(), 0) + 1);
        }
        
        saveUserPreferences();
    }
    
    /**
     * Get personalized search suggestions based on user history
     */
    public List<String> getPersonalizedSuggestions(String query, List<String> baseSuggestions) {
        List<String> personalizedSuggestions = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            // Return most searched queries when no input
            personalizedSuggestions.addAll(getMostSearchedQueries(10));
        } else {
            // Combine base suggestions with personalized ones
            personalizedSuggestions.addAll(baseSuggestions);
            
            // Add frequently searched similar queries
            String normalizedQuery = SearchUtils.removeAccents(query.trim().toLowerCase());
            for (String searchedQuery : searchFrequency.keySet()) {
                if (searchedQuery.contains(normalizedQuery) && !personalizedSuggestions.contains(searchedQuery)) {
                    personalizedSuggestions.add(searchedQuery);
                }
            }
        }
        
        // Sort by relevance and frequency
        Collections.sort(personalizedSuggestions, (s1, s2) -> {
            int freq1 = searchFrequency.getOrDefault(SearchUtils.removeAccents(s1.toLowerCase()), 0);
            int freq2 = searchFrequency.getOrDefault(SearchUtils.removeAccents(s2.toLowerCase()), 0);
            return Integer.compare(freq2, freq1); // Descending order
        });
        
        // Limit results
        if (personalizedSuggestions.size() > 10) {
            personalizedSuggestions = personalizedSuggestions.subList(0, 10);
        }
        
        return personalizedSuggestions;
    }
    
    /**
     * Boost search results based on user preferences
     */
    public List<SearchResult> personalizeSearchResults(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        // Calculate personalized scores for each result
        for (SearchResult result : results) {
            double personalizedScore = calculatePersonalizedScore(result);
            // You can store this score in SearchResult if needed
            Log.d(TAG, "Personalized score for " + result.getTitle() + ": " + personalizedScore);
        }
        
        // Sort by personalized relevance
        Collections.sort(results, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult r1, SearchResult r2) {
                double score1 = calculatePersonalizedScore(r1);
                double score2 = calculatePersonalizedScore(r2);
                return Double.compare(score2, score1); // Descending order
            }
        });
        
        return results;
    }
    
    private double calculatePersonalizedScore(SearchResult result) {
        double score = 0.0;
        
        switch (result.getType()) {
            case SONG:
                Song song = result.getSong();
                if (song != null) {
                    // Boost if song was played before
                    score += playHistory.getOrDefault(song.getSongId(), 0) * 10.0;
                    
                    // Boost if genre is preferred
                    if (song.getGenreId() != null) {
                        score += genrePreferences.getOrDefault(song.getGenreId(), 0) * 5.0;
                    }
                    
                    // Boost if artist is preferred
                    if (song.getArtistId() != null) {
                        score += artistPreferences.getOrDefault(song.getArtistId(), 0) * 7.0;
                    }
                }
                break;
                
            case ARTIST:
                // Boost if artist is preferred
                score += artistPreferences.getOrDefault(result.getId(), 0) * 15.0;
                break;
                
            case ALBUM:
                // Boost based on artist preference (if available)
                // You might need to get artist info from album
                break;
        }
        
        return score;
    }
    
    private List<String> getMostSearchedQueries(int limit) {
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(searchFrequency.entrySet());
        Collections.sort(sortedEntries, (e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
        
        List<String> mostSearched = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sortedEntries.size()); i++) {
            mostSearched.add(sortedEntries.get(i).getKey());
        }
        
        return mostSearched;
    }
    
    /**
     * Get user's favorite genres
     */
    public List<String> getFavoriteGenres(int limit) {
        List<Map.Entry<String, Integer>> sortedGenres = new ArrayList<>(genrePreferences.entrySet());
        Collections.sort(sortedGenres, (e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
        
        List<String> favoriteGenres = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sortedGenres.size()); i++) {
            favoriteGenres.add(sortedGenres.get(i).getKey());
        }
        
        return favoriteGenres;
    }
    
    /**
     * Get user's favorite artists
     */
    public List<String> getFavoriteArtists(int limit) {
        List<Map.Entry<String, Integer>> sortedArtists = new ArrayList<>(artistPreferences.entrySet());
        Collections.sort(sortedArtists, (e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
        
        List<String> favoriteArtists = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sortedArtists.size()); i++) {
            favoriteArtists.add(sortedArtists.get(i).getKey());
        }
        
        return favoriteArtists;
    }
    
    /**
     * Clear all user data (for privacy/reset)
     */
    public void clearUserData() {
        searchFrequency.clear();
        playHistory.clear();
        genrePreferences.clear();
        artistPreferences.clear();
        saveUserPreferences();
        Log.d(TAG, "Cleared all user data");
    }
    
    /**
     * Get statistics for debugging
     */
    public String getStatistics() {
        return String.format("Searches: %d, Plays: %d, Genres: %d, Artists: %d",
                searchFrequency.size(), playHistory.size(), 
                genrePreferences.size(), artistPreferences.size());
    }
} 