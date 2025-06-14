package com.ck.music_app.MainFragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.Adapter.SearchAdapter;
import com.ck.music_app.Adapter.SearchHistoryAdapter;
import com.ck.music_app.Adapter.SearchResultAdapter;
import com.ck.music_app.Adapter.SearchSuggestionAdapter;
import com.ck.music_app.Model.Album;
import com.ck.music_app.Model.Artist;
import com.ck.music_app.Model.SearchHistory;
import com.ck.music_app.Model.SearchResult;
import com.ck.music_app.Model.Song;
import com.ck.music_app.PlayMusicActivity;
import com.ck.music_app.R;
import com.ck.music_app.utils.SearchUtils;
import com.ck.music_app.utils.SearchHistoryManager;
import com.ck.music_app.utils.VoiceSearchHelper;
import com.ck.music_app.utils.PersonalizedSearchHelper;
import com.ck.music_app.utils.EmotionGenreSearchHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFragment extends Fragment implements
        SearchHistoryAdapter.OnHistoryClickListener,
        SearchSuggestionAdapter.OnSuggestionClickListener,
        SearchResultAdapter.OnResultClickListener,
        VoiceSearchHelper.VoiceSearchListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // Views
    private TextInputEditText searchEditText;
    private RecyclerView searchResultsRecyclerView;
    private RecyclerView recentSearchesRecyclerView;
    private RecyclerView suggestionsRecyclerView;
    private TextView recentSearchesTitle;
    private TextView suggestionsTitle;
    private View emptyStateContainer;
    private ImageView voiceSearchButton;

    // Adapters
    private SearchAdapter searchAdapter; // Keep for backward compatibility
    private SearchResultAdapter searchResultAdapter;
    private SearchHistoryAdapter historyAdapter;
    private SearchSuggestionAdapter suggestionAdapter;

    // Data and managers
    private SearchHistoryManager historyManager;
    private VoiceSearchHelper voiceSearchHelper;
    private PersonalizedSearchHelper personalizedSearchHelper;
    private FirebaseFirestore db;
    private List<Song> searchResults;
    private List<SearchResult> allSearchResults;
    private List<SearchHistory> recentSearches;
    private List<String> suggestions;
    private List<String> allTitles; // Cache for suggestions

    // Search handling
    private Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY = 300; // Delay 300ms để tránh gọi quá nhiều lần
    private static final double FUZZY_THRESHOLD = 0.6; // Threshold cho fuzzy search

    public SearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SearchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Initialize views
        searchEditText = view.findViewById(R.id.searchEditText);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        recentSearchesRecyclerView = view.findViewById(R.id.recentSearchesRecyclerView);
        suggestionsRecyclerView = view.findViewById(R.id.suggestionsRecyclerView);
        recentSearchesTitle = view.findViewById(R.id.recentSearchesTitle);
        suggestionsTitle = view.findViewById(R.id.suggestionsTitle);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        voiceSearchButton = view.findViewById(R.id.voiceSearchButton);

        // Initialize managers and data
        db = FirebaseFirestore.getInstance();
        historyManager = new SearchHistoryManager(getContext());
        voiceSearchHelper = new VoiceSearchHelper(getContext(), this);
        personalizedSearchHelper = new PersonalizedSearchHelper(getContext());
        searchResults = new ArrayList<>();
        allSearchResults = new ArrayList<>();
        recentSearches = new ArrayList<>();
        suggestions = new ArrayList<>();
        allTitles = new ArrayList<>();
        searchHandler = new Handler(Looper.getMainLooper());

        // Setup RecyclerViews
        setupSearchResultsRecyclerView();
        setupRecentSearchesRecyclerView();
        setupSuggestionsRecyclerView();

        // Setup search functionality
        setupSearchListener();
        setupVoiceSearch();

        // Load initial data
        loadRecentSearches();
        loadAllTitlesForSuggestions();

        return view;
    }

    private void setupSearchResultsRecyclerView() {
        // Keep old adapter for backward compatibility
        searchAdapter = new SearchAdapter(searchResults, song -> {
            Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
            intent.putExtra("songList", (Serializable) searchResults);
            intent.putExtra("currentIndex", searchResults.indexOf(song));
            startActivity(intent);
        });

        // New unified search result adapter
        searchResultAdapter = new SearchResultAdapter(allSearchResults, this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecyclerView.setAdapter(searchResultAdapter);
    }

    private void setupRecentSearchesRecyclerView() {
        historyAdapter = new SearchHistoryAdapter(recentSearches, this);
        recentSearchesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentSearchesRecyclerView.setAdapter(historyAdapter);
    }

    private void setupSuggestionsRecyclerView() {
        suggestionAdapter = new SearchSuggestionAdapter(suggestions, this);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        suggestionsRecyclerView.setAdapter(suggestionAdapter);
    }

    private void loadRecentSearches() {
        recentSearches.clear();
        recentSearches.addAll(historyManager.getRecentSearches());
        historyAdapter.updateHistory(recentSearches);

        // Hiển thị/ẩn section recent searches
        boolean hasHistory = !recentSearches.isEmpty();
        recentSearchesTitle.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
        recentSearchesRecyclerView.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    String searchText = s.toString().trim();
                    if (searchText.isEmpty()) {
                        // Hiển thị recent searches khi không có từ khóa
                        showRecentSearches();
                        return;
                    }

                    // Hiển thị gợi ý khi đang gõ
                    if (searchText.length() >= 1) {
                        showSuggestions(searchText);
                    }

                    // Tìm kiếm khi có ít nhất 2 ký tự
                    if (searchText.length() >= 2) {
                        performAdvancedSearch(searchText);
                    }
                };

                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void showRecentSearches() {
        // Clear search results
        searchResults.clear();
        allSearchResults.clear();
        searchAdapter.notifyDataSetChanged();
        searchResultAdapter.updateResults(allSearchResults);

        // Hiển thị recent searches
        loadRecentSearches();

        // Ẩn gợi ý
        suggestionsTitle.setVisibility(View.GONE);
        suggestionsRecyclerView.setVisibility(View.GONE);

        // Ẩn kết quả tìm kiếm và empty state
        searchResultsRecyclerView.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.GONE);
    }

    private void searchSongs(String searchText) {
        // Ẩn recent searches khi đang tìm kiếm
        recentSearchesTitle.setVisibility(View.GONE);
        recentSearchesRecyclerView.setVisibility(View.GONE);

        // Lưu vào lịch sử tìm kiếm
        historyManager.addSearchQuery(searchText);

        String searchLower = searchText.toLowerCase();
        Log.d("SearchFragment", "Searching for songs containing: " + searchLower);

        db.collection("songs")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        searchResults.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            if (title != null && title.toLowerCase().contains(searchLower)) {
                                try {
                                    Song song = new Song();
                                    song.setSongId(document.getId());
                                    song.setTitle(document.getString("title"));
                                    song.setArtistId(document.getString("artistId"));
                                    song.setAudioUrl(document.getString("audioUrl"));
                                    song.setCoverUrl(document.getString("coverUrl"));
                                    song.setCreateAt(document.getString("createAt"));
                                    song.setDuration(document.getLong("duration") != null
                                            ? document.getLong("duration").intValue()
                                            : 0);
                                    song.setGenreId(document.getString("genreId"));
                                    song.setLikeCount(document.getLong("likeCount") != null
                                            ? document.getLong("likeCount").intValue()
                                            : 0);
                                    song.setViewCount(document.getLong("viewCount") != null
                                            ? document.getLong("viewCount").intValue()
                                            : 0);

                                    // Xử lý albumId có thể là String hoặc List
                                    Object albumIdObj = document.get("albumId");
                                    if (albumIdObj instanceof List) {
                                        List<String> albumIds = (List<String>) albumIdObj;
                                        if (!albumIds.isEmpty()) {
                                            song.setAlbumIds(albumIds);
                                        }
                                    } else if (albumIdObj instanceof String) {
                                        song.setAlbumId((String) albumIdObj);
                                    }

                                    searchResults.add(song);
                                    Log.d("SearchFragment", "Found song: " + song.getTitle());
                                } catch (Exception e) {
                                    Log.e("SearchFragment", "Error parsing song document: " + document.getId(), e);
                                }
                            }
                        }

                        // Cập nhật UI
                        if (searchResults.isEmpty()) {
                            emptyStateContainer.setVisibility(View.VISIBLE);
                            searchResultsRecyclerView.setVisibility(View.GONE);
                        } else {
                            emptyStateContainer.setVisibility(View.GONE);
                            searchResultsRecyclerView.setVisibility(View.VISIBLE);
                        }
                        searchAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("SearchFragment", "Error searching songs", task.getException());
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        searchResultsRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    // Implement SearchHistoryAdapter.OnHistoryClickListener
    @Override
    public void onHistoryClick(String query) {
        // Set text vào search box và tìm kiếm
        searchEditText.setText(query);
        searchEditText.setSelection(query.length());
        searchSongs(query);
    }

    @Override
    public void onHistoryRemove(String query) {
        // Xóa khỏi lịch sử
        historyManager.removeSearchHistory(query);
        loadRecentSearches();
    }

    // ==================== NEW ADVANCED SEARCH METHODS ====================

    private void loadAllTitlesForSuggestions() {
        // Load all song titles, artist names, album titles for suggestions
        db.collection("songs").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Set<String> titleSet = new HashSet<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String title = document.getString("title");
                    if (title != null && !title.trim().isEmpty()) {
                        titleSet.add(title.trim());
                    }
                }
                allTitles.clear();
                allTitles.addAll(titleSet);
            }
        });

        // Also load artist names and album titles
        loadArtistNamesForSuggestions();
        loadAlbumTitlesForSuggestions();
    }

    private void loadArtistNamesForSuggestions() {
        db.collection("artists").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String name = document.getString("name");
                    if (name != null && !name.trim().isEmpty()) {
                        allTitles.add(name.trim());
                    }
                }
            }
        });
    }

    private void loadAlbumTitlesForSuggestions() {
        db.collection("albums").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String title = document.getString("title");
                    if (title != null && !title.trim().isEmpty()) {
                        allTitles.add(title.trim());
                    }
                }
            }
        });
    }

    private void showSuggestions(String query) {
        suggestions.clear();

        // Get base suggestions
        List<String> baseSuggestions = SearchUtils.generateSuggestions(query, allTitles);

        // Get emotion/genre suggestions
        List<String> emotionGenreSuggestions = EmotionGenreSearchHelper.getEmotionGenreSuggestions(query);

        // Get personalized suggestions
        List<String> personalizedSuggestions = personalizedSearchHelper.getPersonalizedSuggestions(query,
                baseSuggestions);

        // Combine all suggestions (emotion/genre first, then personalized, then base)
        suggestions.addAll(emotionGenreSuggestions);
        for (String suggestion : personalizedSuggestions) {
            if (!suggestions.contains(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        if (!suggestions.isEmpty()) {
            suggestionsTitle.setVisibility(View.VISIBLE);
            suggestionsRecyclerView.setVisibility(View.VISIBLE);
            suggestionAdapter.updateSuggestions(suggestions);
        } else {
            suggestionsTitle.setVisibility(View.GONE);
            suggestionsRecyclerView.setVisibility(View.GONE);
        }

        // Ẩn recent searches khi có gợi ý
        recentSearchesTitle.setVisibility(View.GONE);
        recentSearchesRecyclerView.setVisibility(View.GONE);
    }

    private void performAdvancedSearch(String searchText) {
        // Ẩn gợi ý và recent searches
        hideSuggestionsAndHistory();

        // Lưu vào lịch sử tìm kiếm
        historyManager.addSearchQuery(searchText);

        // Record search for personalization
        personalizedSearchHelper.recordSearch(searchText);

        Log.d("SearchFragment", "Performing advanced search for: " + searchText);

        // Tìm kiếm đồng thời songs, artists, albums
        searchSongsAdvanced(searchText);
        searchArtists(searchText);
        searchAlbums(searchText);
    }

    private void searchSongsAdvanced(String searchText) {
        db.collection("songs").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<SearchResult> songResults = new ArrayList<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String title = document.getString("title");
                    String artistId = document.getString("artistId");

                    // Fuzzy search với title và artistId
                    boolean titleMatch = SearchUtils.isFuzzyMatch(searchText, title, FUZZY_THRESHOLD);
                    boolean artistMatch = SearchUtils.isFuzzyMatch(searchText, artistId, FUZZY_THRESHOLD);

                    if (titleMatch || artistMatch) {
                        try {
                            Song song = new Song();
                            song.setSongId(document.getId());
                            song.setTitle(document.getString("title"));
                            song.setArtistId(document.getString("artistId"));
                            song.setAudioUrl(document.getString("audioUrl"));
                            song.setCoverUrl(document.getString("coverUrl"));
                            song.setCreateAt(document.getString("createAt"));
                            song.setDuration(document.getLong("duration") != null
                                    ? document.getLong("duration").intValue()
                                    : 0);
                            song.setGenreId(document.getString("genreId"));
                            song.setLikeCount(document.getLong("likeCount") != null
                                    ? document.getLong("likeCount").intValue()
                                    : 0);
                            song.setViewCount(document.getLong("viewCount") != null
                                    ? document.getLong("viewCount").intValue()
                                    : 0);

                            // Xử lý albumId có thể là String hoặc List
                            Object albumIdObj = document.get("albumId");
                            if (albumIdObj instanceof List) {
                                List<String> albumIds = (List<String>) albumIdObj;
                                if (!albumIds.isEmpty()) {
                                    song.setAlbumIds(albumIds);
                                }
                            } else if (albumIdObj instanceof String) {
                                song.setAlbumId((String) albumIdObj);
                            }

                            SearchResult result = SearchResult.fromSong(song);
                            songResults.add(result);

                        } catch (Exception e) {
                            Log.e("SearchFragment", "Error parsing song: " + document.getId(), e);
                        }
                    }
                }

                // Sắp xếp theo relevance score
                Collections.sort(songResults, (r1, r2) -> {
                    double score1 = SearchUtils.calculateRelevanceScore(searchText, r1.getTitle(), r1.getSubtitle());
                    double score2 = SearchUtils.calculateRelevanceScore(searchText, r2.getTitle(), r2.getSubtitle());

                    // Add emotion/genre scoring if applicable
                    if (EmotionGenreSearchHelper.isEmotionGenreQuery(searchText)) {
                        Song song1 = r1.getSong();
                        Song song2 = r2.getSong();
                        if (song1 != null) {
                            score1 += EmotionGenreSearchHelper.calculateEmotionGenreScore(
                                    searchText, song1.getTitle(), song1.getArtistId(), song1.getGenreId());
                        }
                        if (song2 != null) {
                            score2 += EmotionGenreSearchHelper.calculateEmotionGenreScore(
                                    searchText, song2.getTitle(), song2.getArtistId(), song2.getGenreId());
                        }
                    }

                    return Double.compare(score2, score1); // Descending order
                });

                updateSearchResults(songResults, SearchResult.Type.SONG);
            }
        });
    }

    private void searchArtists(String searchText) {
        db.collection("artists").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<SearchResult> artistResults = new ArrayList<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String name = document.getString("name");
                    String bio = document.getString("bio");

                    boolean nameMatch = SearchUtils.isFuzzyMatch(searchText, name, FUZZY_THRESHOLD);
                    boolean bioMatch = SearchUtils.isFuzzyMatch(searchText, bio, FUZZY_THRESHOLD);

                    if (nameMatch || bioMatch) {
                        try {
                            Artist artist = new Artist();
                            artist.setArtistId(document.getId());
                            artist.setName(document.getString("name"));
                            artist.setBio(document.getString("bio"));
                            artist.setImageUrl(document.getString("imageUrl"));
                            artist.setFollowerCount(document.getLong("followerCount") != null
                                    ? document.getLong("followerCount").intValue()
                                    : null);
                            artist.setGenreId(document.getString("genreId"));
                            artist.setCreateAt(document.getString("createAt"));

                            SearchResult result = SearchResult.fromArtist(artist);
                            artistResults.add(result);

                        } catch (Exception e) {
                            Log.e("SearchFragment", "Error parsing artist: " + document.getId(), e);
                        }
                    }
                }

                Collections.sort(artistResults, (r1, r2) -> {
                    double score1 = SearchUtils.calculateRelevanceScore(searchText, r1.getTitle(), r1.getSubtitle());
                    double score2 = SearchUtils.calculateRelevanceScore(searchText, r2.getTitle(), r2.getSubtitle());
                    return Double.compare(score2, score1);
                });

                updateSearchResults(artistResults, SearchResult.Type.ARTIST);
            }
        });
    }

    private void searchAlbums(String searchText) {
        db.collection("albums").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<SearchResult> albumResults = new ArrayList<>();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String title = document.getString("title");
                    String artistId = document.getString("artistId");

                    boolean titleMatch = SearchUtils.isFuzzyMatch(searchText, title, FUZZY_THRESHOLD);
                    boolean artistMatch = SearchUtils.isFuzzyMatch(searchText, artistId, FUZZY_THRESHOLD);

                    if (titleMatch || artistMatch) {
                        try {
                            Album album = new Album();
                            album.setAlbumId(document.getId());
                            album.setTitle(document.getString("title"));
                            album.setArtistId(document.getString("artistId"));
                            album.setCoverUrl(document.getString("coverUrl"));
                            album.setReleaseDate(document.getString("releaseDate"));
                            album.setGenreId(document.getString("genreId"));
                            album.setTrackCount(document.getLong("trackCount") != null
                                    ? document.getLong("trackCount").intValue()
                                    : null);
                            album.setCreateAt(document.getString("createAt"));

                            SearchResult result = SearchResult.fromAlbum(album);
                            albumResults.add(result);

                        } catch (Exception e) {
                            Log.e("SearchFragment", "Error parsing album: " + document.getId(), e);
                        }
                    }
                }

                Collections.sort(albumResults, (r1, r2) -> {
                    double score1 = SearchUtils.calculateRelevanceScore(searchText, r1.getTitle(), r1.getSubtitle());
                    double score2 = SearchUtils.calculateRelevanceScore(searchText, r2.getTitle(), r2.getSubtitle());
                    return Double.compare(score2, score1);
                });

                updateSearchResults(albumResults, SearchResult.Type.ALBUM);
            }
        });
    }

    private void updateSearchResults(List<SearchResult> newResults, SearchResult.Type type) {
        // Remove existing results of this type
        allSearchResults.removeIf(result -> result.getType() == type);

        // Add new results
        allSearchResults.addAll(newResults);

        // Apply personalized sorting
        allSearchResults = personalizedSearchHelper.personalizeSearchResults(allSearchResults);

        // Update UI
        if (allSearchResults.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }

        searchResultAdapter.updateResults(allSearchResults);
    }

    private void hideSuggestionsAndHistory() {
        suggestionsTitle.setVisibility(View.GONE);
        suggestionsRecyclerView.setVisibility(View.GONE);
        recentSearchesTitle.setVisibility(View.GONE);
        recentSearchesRecyclerView.setVisibility(View.GONE);
    }

    // ==================== INTERFACE IMPLEMENTATIONS ====================

    // SearchSuggestionAdapter.OnSuggestionClickListener
    @Override
    public void onSuggestionClick(String suggestion) {
        searchEditText.setText(suggestion);
        searchEditText.setSelection(suggestion.length());
        performAdvancedSearch(suggestion);
    }

    @Override
    public void onInsertClick(String suggestion) {
        searchEditText.setText(suggestion);
        searchEditText.setSelection(suggestion.length());
    }

    // SearchResultAdapter.OnResultClickListener
    @Override
    public void onResultClick(SearchResult result) {
        switch (result.getType()) {
            case SONG:
                Song song = result.getSong();
                if (song != null) {
                    // Record song play for personalization
                    personalizedSearchHelper.recordSongPlay(song);

                    // Create song list from current search results
                    List<Song> songList = new ArrayList<>();
                    for (SearchResult sr : allSearchResults) {
                        if (sr.getType() == SearchResult.Type.SONG) {
                            songList.add(sr.getSong());
                        }
                    }

                    Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
                    intent.putExtra("songList", (Serializable) songList);
                    intent.putExtra("currentIndex", songList.indexOf(song));
                    startActivity(intent);
                }
                break;
            case ARTIST:
                // TODO: Navigate to artist detail page
                Log.d("SearchFragment", "Artist clicked: " + result.getTitle());
                break;
            case ALBUM:
                // TODO: Navigate to album detail page
                Log.d("SearchFragment", "Album clicked: " + result.getTitle());
                break;
        }
    }

    @Override
    public void onActionClick(SearchResult result) {
        switch (result.getType()) {
            case SONG:
                // Play song immediately
                onResultClick(result);
                break;
            case ARTIST:
                // TODO: Follow/unfollow artist
                Log.d("SearchFragment", "Artist action clicked: " + result.getTitle());
                break;
            case ALBUM:
                // TODO: Play album or add to library
                Log.d("SearchFragment", "Album action clicked: " + result.getTitle());
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (voiceSearchHelper != null) {
            voiceSearchHelper.destroy();
        }
    }

    private void setupVoiceSearch() {
        // Check if voice search is available
        if (!VoiceSearchHelper.isVoiceSearchAvailable(getContext())) {
            voiceSearchButton.setVisibility(View.GONE);
            return;
        }

        voiceSearchButton.setOnClickListener(v -> {
            if (voiceSearchHelper.isListening()) {
                voiceSearchHelper.stopListening();
            } else {
                if (voiceSearchHelper.checkPermission()) {
                    voiceSearchHelper.startListening();
                } else {
                    voiceSearchHelper.requestPermission(getActivity());
                }
            }
        });
    }

    // ==================== VOICE SEARCH LISTENER METHODS ====================

    @Override
    public void onVoiceSearchStart() {
        Log.d("SearchFragment", "Voice search started");
        // Change mic icon to indicate listening
        voiceSearchButton.setImageResource(R.drawable.ic_mic);
        voiceSearchButton.setAlpha(1.0f);
        // You can add animation here
    }

    @Override
    public void onVoiceSearchListening() {
        Log.d("SearchFragment", "Voice search is listening");
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), "🎤 Đã bật tìm kiếm bằng giọng nói - Hãy nói...",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
        // Change mic icon to indicate active listening
        voiceSearchButton.setImageResource(R.drawable.ic_mic);
        voiceSearchButton.setAlpha(1.0f);
    }

    @Override
    public void onVoiceDetected() {
        Log.d("SearchFragment", "Voice detected");
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), "✅ Đã nhận được giọng nói - Đang xử lý...",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVoiceSearchResult(String result) {
        Log.d("SearchFragment", "Voice search result: " + result);
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), "🔍 Tìm kiếm: " + result, android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
        // Set the result to search box and perform search
        searchEditText.setText(result);
        searchEditText.setSelection(result.length());
        performAdvancedSearch(result);
    }

    @Override
    public void onVoiceSearchError(String error) {
        Log.e("SearchFragment", "Voice search error: " + error);
        // Show error message to user (you can use Toast or Snackbar)
        if (getContext() != null) {
            String errorMessage;
            if (error.contains("Không nghe thấy giọng nói") || error.contains("Không nhận diện được giọng nói")) {
                errorMessage = "❌ Không nhận được giọng nói - Vui lòng thử lại";
            } else {
                errorMessage = "❌ " + error;
            }
            android.widget.Toast.makeText(getContext(), errorMessage, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onVoiceSearchEnd() {
        Log.d("SearchFragment", "Voice search ended");
        // Reset mic icon
        voiceSearchButton.setImageResource(R.drawable.ic_mic);
        voiceSearchButton.setAlpha(0.6f);
    }
}