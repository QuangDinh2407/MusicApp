package com.ck.music_app.MainFragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Adapter.RecyclerViewAdapterSong;
import com.ck.music_app.Adapter.SearchHistoryAdapter;
import com.ck.music_app.Adapter.SearchArtistAdapter;
import com.ck.music_app.Adapter.SearchAlbumAdapter;
import com.ck.music_app.Adapter.SearchPlaylistAdapter;
import com.ck.music_app.MainActivity;
import com.ck.music_app.MainFragment.HomeChildFragment.AlbumSongsFragment;
import com.ck.music_app.MainFragment.HomeChildFragment.PlaylistSongsFragment;
import com.ck.music_app.Model.Album;
import com.ck.music_app.Model.Artist;
import com.ck.music_app.Model.Playlist;
import com.ck.music_app.Model.SearchFilter;
import com.ck.music_app.Model.SearchResult;
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.FirebaseService;
import com.ck.music_app.Services.MusicService;
import com.ck.music_app.interfaces.OnSongClickListener;
import com.ck.music_app.utils.FirestoreUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFragment extends Fragment
        implements OnSongClickListener, SearchHistoryAdapter.OnHistoryClickListener,
        SearchArtistAdapter.OnArtistClickListener, SearchAlbumAdapter.OnAlbumClickListener,
        SearchPlaylistAdapter.OnPlaylistClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private SearchView searchView;

    // Filter components
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipSongs, chipArtists, chipAlbums, chipPlaylists;
    private SearchFilter currentFilter = SearchFilter.ALL;

    // Search result views
    private NestedScrollView scrollViewResults;
    private TextView tvNoResults;

    // Top result
    private LinearLayout layoutTopResult;
    private ImageView imgTopResultCover, imgTopResultPlay;
    private TextView tvTopResultTitle, tvTopResultSubtitle, tvTopResultType;

    // Category sections
    private LinearLayout layoutSongs, layoutArtists, layoutAlbums, layoutPlaylists;
    private RecyclerView recyclerViewSongs, recyclerViewArtists, recyclerViewAlbums, recyclerViewPlaylists;

    // History and suggestions
    private RecyclerView recyclerViewHistory, recyclerViewSuggestions;

    // Adapters
    private RecyclerViewAdapterSong songAdapter;
    private SearchHistoryAdapter historyAdapter;
    private SearchArtistAdapter artistAdapter;
    private SearchAlbumAdapter albumAdapter;
    private SearchPlaylistAdapter playlistAdapter;

    // Data
    private FirebaseService firebaseService;
    private SharedPreferences searchPreferences;
    private static final String SEARCH_HISTORY_KEY = "search_history";
    private static final int MAX_HISTORY_SIZE = 10;
    private List<String> searchHistory;

    // Current search result
    private SearchResult currentSearchResult;
    private String currentQuery = "";
    private String previousQuery = "";
    private boolean hasPerformedSearch = false;
    private static final int MIN_QUERY_LENGTH_FOR_HISTORY = 2;

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
    public void onPause() {
        super.onPause();
        // Save current query to history when user leaves search fragment
        savePreviousQueryToHistory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Save current query to history when view is destroyed
        savePreviousQueryToHistory();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Kiểm tra nếu không có fragment con nào trong stack
        if (getChildFragmentManager().getBackStackEntryCount() == 0) {
            View mainContent = getView().findViewById(R.id.mainSearchContent);
            View container = getView().findViewById(R.id.albumSongsContainer);
            if (mainContent != null && container != null) {
                mainContent.setVisibility(View.VISIBLE);
                container.setVisibility(View.GONE);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        initViews(view);
        initData();
        setupSearchView();
        setupFilterChips();
        loadSearchHistory();
        showSearchHistory();

        return view;
    }

    private void initViews(View view) {
        searchView = view.findViewById(R.id.searchView);

        // Filter chips
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipAll = view.findViewById(R.id.chipAll);
        chipSongs = view.findViewById(R.id.chipSongs);
        chipArtists = view.findViewById(R.id.chipArtists);
        chipAlbums = view.findViewById(R.id.chipAlbums);
        chipPlaylists = view.findViewById(R.id.chipPlaylists);

        // Result views
        scrollViewResults = view.findViewById(R.id.scrollViewResults);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        // Top result
        layoutTopResult = view.findViewById(R.id.layoutTopResult);
        imgTopResultCover = view.findViewById(R.id.imgTopResultCover);
        imgTopResultPlay = view.findViewById(R.id.imgTopResultPlay);
        tvTopResultTitle = view.findViewById(R.id.tvTopResultTitle);
        tvTopResultSubtitle = view.findViewById(R.id.tvTopResultSubtitle);
        tvTopResultType = view.findViewById(R.id.tvTopResultType);

        // Category sections
        layoutSongs = view.findViewById(R.id.layoutSongs);
        layoutArtists = view.findViewById(R.id.layoutArtists);
        layoutAlbums = view.findViewById(R.id.layoutAlbums);
        layoutPlaylists = view.findViewById(R.id.layoutPlaylists);

        recyclerViewSongs = view.findViewById(R.id.recyclerViewSongs);
        recyclerViewArtists = view.findViewById(R.id.recyclerViewArtists);
        recyclerViewAlbums = view.findViewById(R.id.recyclerViewAlbums);
        recyclerViewPlaylists = view.findViewById(R.id.recyclerViewPlaylists);

        // History and suggestions
        recyclerViewHistory = view.findViewById(R.id.recyclerViewHistory);
        recyclerViewSuggestions = view.findViewById(R.id.recyclerViewSuggestions);

        // Setup RecyclerViews
        setupRecyclerViews();
    }

    private void initData() {
        firebaseService = FirebaseService.getInstance();
        searchPreferences = getActivity().getSharedPreferences("search_prefs", getActivity().MODE_PRIVATE);
        searchHistory = new ArrayList<>();
    }

    private void setupRecyclerViews() {
        try {
            // Songs RecyclerView
            songAdapter = new RecyclerViewAdapterSong(getContext(), new ArrayList<>());
            recyclerViewSongs.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewSongs.setAdapter(songAdapter);
            songAdapter.setOnSongClickListener(this);

            // Artists RecyclerView
            artistAdapter = new SearchArtistAdapter(getContext(), new ArrayList<>());
            artistAdapter.setOnArtistClickListener(this);
            recyclerViewArtists
                    .setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            recyclerViewArtists.setAdapter(artistAdapter);

            // Albums RecyclerView
            albumAdapter = new SearchAlbumAdapter(getContext(), new ArrayList<>());
            albumAdapter.setOnAlbumClickListener(this);
            recyclerViewAlbums.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewAlbums.setAdapter(albumAdapter);

            // Playlists RecyclerView
            playlistAdapter = new SearchPlaylistAdapter(getContext(), new ArrayList<>());
            playlistAdapter.setOnPlaylistClickListener(this);
            recyclerViewPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewPlaylists.setAdapter(playlistAdapter);

            // History RecyclerView
            historyAdapter = new SearchHistoryAdapter(searchHistory);
            historyAdapter.setOnHistoryClickListener(this);
            recyclerViewHistory.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewHistory.setAdapter(historyAdapter);

            // Suggestions RecyclerView
            recyclerViewSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        } catch (Exception e) {
            Log.e("SearchFragment", "Error setting up RecyclerViews: " + e.getMessage());
        }
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                saveSearchHistoryIfValid(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Real-time logic: save previous query to history if conditions met
                handleQueryChange(newText);

                currentQuery = newText;

                if (newText.length() > 0) {
                    hideSearchHistory();
                    if (newText.length() >= 2) {
                        // Perform search for queries with 2+ characters
                        performSearch(newText);
                        hasPerformedSearch = true;
                    } else {
                        showSearchSuggestions(newText);
                    }
                } else {
                    // When query is cleared, save previous query to history
                    savePreviousQueryToHistory();
                    hideSearchResults();
                    showSearchHistory();
                }
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            // Save current query to history when closing search
            savePreviousQueryToHistory();
            hideSearchResults();
            showSearchHistory();
            resetSearchState();
            return false;
        });

        // Add focus change listener for additional real-time scenarios
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // User left search view, save current query if valid
                savePreviousQueryToHistory();
            }
        });
    }

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // If no chip is selected, default to ALL
                chipAll.setChecked(true);
                currentFilter = SearchFilter.ALL;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipAll) {
                    currentFilter = SearchFilter.ALL;
                } else if (checkedId == R.id.chipSongs) {
                    currentFilter = SearchFilter.SONGS;
                } else if (checkedId == R.id.chipArtists) {
                    currentFilter = SearchFilter.ARTISTS;
                } else if (checkedId == R.id.chipAlbums) {
                    currentFilter = SearchFilter.ALBUMS;
                } else if (checkedId == R.id.chipPlaylists) {
                    currentFilter = SearchFilter.PLAYLISTS;
                }
            }

            // Re-apply filter to current search results
            if (currentSearchResult != null && !currentQuery.isEmpty()) {
                applyFilterToResults(currentSearchResult);
            }
        });
    }

    private void loadSearchHistory() {
        try {
            // Try to load as JSON string first (new format)
            String historyJson = searchPreferences.getString(SEARCH_HISTORY_KEY, "");
            if (!historyJson.isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<String>>() {
                }.getType();
                List<String> loadedHistory = gson.fromJson(historyJson, type);
                if (loadedHistory != null) {
                    searchHistory.clear();
                    searchHistory.addAll(loadedHistory);
                }
            }
        } catch (Exception e) {
            try {
                // Fallback: try to load as StringSet (old format)
                Set<String> historySet = searchPreferences.getStringSet(SEARCH_HISTORY_KEY, new HashSet<>());
                if (historySet != null && !historySet.isEmpty()) {
                    searchHistory.clear();
                    searchHistory.addAll(historySet);

                    // Convert to new format immediately
                    Gson gson = new Gson();
                    String historyJson = gson.toJson(searchHistory);
                    searchPreferences.edit().putString(SEARCH_HISTORY_KEY, historyJson).apply();
                }
            } catch (Exception e2) {
                // If both fail, clear the corrupted data
                searchPreferences.edit().remove(SEARCH_HISTORY_KEY).apply();
                searchHistory.clear();
            }
        }
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            hideSearchResults();
            showSearchHistory();
            return;
        }

        hideSearchHistory();
        hideSuggestions();

        firebaseService.searchAll(query, new FirebaseService.FirestoreCallback<SearchResult>() {
            @Override
            public void onSuccess(SearchResult searchResult) {
                currentSearchResult = searchResult;
                applyFilterToResults(searchResult);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Lỗi khi tìm kiếm", Toast.LENGTH_SHORT).show();
                showNoResults();
            }
        });
    }

    private void applyFilterToResults(SearchResult searchResult) {
        SearchResult filteredResult = filterSearchResult(searchResult, currentFilter);
        displaySearchResults(filteredResult);
    }

    private SearchResult filterSearchResult(SearchResult originalResult, SearchFilter filter) {
        SearchResult filteredResult = new SearchResult();

        switch (filter) {
            case ALL:
                return originalResult; // Return all results

            case SONGS:
                filteredResult.setSongs(originalResult.getSongs());
                filteredResult.setTopSong(originalResult.getTopSong());
                break;

            case ARTISTS:
                filteredResult.setArtists(originalResult.getArtists());
                filteredResult.setTopArtist(originalResult.getTopArtist());
                break;

            case ALBUMS:
                filteredResult.setAlbums(originalResult.getAlbums());
                break;

            case PLAYLISTS:
                filteredResult.setPlaylists(originalResult.getPlaylists());
                break;
        }

        return filteredResult;
    }

    private void displaySearchResults(SearchResult searchResult) {
        if (!searchResult.hasResults()) {
            showNoResults();
            return;
        }

        hideNoResults();
        showSearchResults();

        // Display based on current filter
        switch (currentFilter) {
            case ALL:
                displayAllResults(searchResult);
                break;
            case SONGS:
                displaySongsOnly(searchResult);
                break;
            case ARTISTS:
                displayArtistsOnly(searchResult);
                break;
            case ALBUMS:
                displayAlbumsOnly(searchResult);
                break;
            case PLAYLISTS:
                displayPlaylistsOnly(searchResult);
                break;
        }
    }

    private void displayAllResults(SearchResult searchResult) {
        // Display top result
        displayTopResult(searchResult);

        // Display categorized results
        displaySongs(searchResult.getSongs());
        displayArtists(searchResult.getArtists());
        displayAlbums(searchResult.getAlbums());
        displayPlaylists(searchResult.getPlaylists());
    }

    private void displaySongsOnly(SearchResult searchResult) {
        hideAllSections();

        if (searchResult.getTopSong() != null) {
            displayTopResultSong(searchResult.getTopSong());
            layoutTopResult.setVisibility(View.VISIBLE);
        }

        displaySongs(searchResult.getSongs());
    }

    private void displayArtistsOnly(SearchResult searchResult) {
        hideAllSections();

        if (searchResult.getTopArtist() != null) {
            displayTopResultArtist(searchResult.getTopArtist());
            layoutTopResult.setVisibility(View.VISIBLE);
        }

        displayArtists(searchResult.getArtists());
    }

    private void displayAlbumsOnly(SearchResult searchResult) {
        hideAllSections();
        displayAlbums(searchResult.getAlbums());
    }

    private void displayPlaylistsOnly(SearchResult searchResult) {
        hideAllSections();
        displayPlaylists(searchResult.getPlaylists());
    }

    private void hideAllSections() {
        layoutTopResult.setVisibility(View.GONE);
        layoutSongs.setVisibility(View.GONE);
        layoutArtists.setVisibility(View.GONE);
        layoutAlbums.setVisibility(View.GONE);
        layoutPlaylists.setVisibility(View.GONE);
    }

    private void displayTopResult(SearchResult searchResult) {
        // Determine the best top result
        if (searchResult.getTopSong() != null) {
            displayTopResultSong(searchResult.getTopSong());
            layoutTopResult.setVisibility(View.VISIBLE);
        } else if (searchResult.getTopArtist() != null) {
            displayTopResultArtist(searchResult.getTopArtist());
            layoutTopResult.setVisibility(View.VISIBLE);
        } else {
            layoutTopResult.setVisibility(View.GONE);
        }
    }

    private void displayTopResultSong(Song song) {
        try {
            if (song == null) {
                Log.e("SearchFragment", "Song is null in displayTopResultSong");
                return;
            }

            // Safely set text with null checks
            if (tvTopResultTitle != null) {
                tvTopResultTitle.setText(song.getTitle() != null ? song.getTitle() : "Unknown Title");
            }
            if (tvTopResultSubtitle != null) {
                tvTopResultSubtitle.setText(song.getArtistId() != null ? song.getArtistId() : "Unknown Artist");
            }
            if (tvTopResultType != null) {
                tvTopResultType.setText("BÀI HÁT");
            }

            // Safely load image
            if (imgTopResultCover != null && getContext() != null) {
                Glide.with(this)
                        .load(song.getCoverUrl())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(imgTopResultCover);
            }

            // Safely set click listener
            if (imgTopResultPlay != null) {
                imgTopResultPlay.setOnClickListener(v -> {
                    try {
                        // Validate activity and context before playing
                        if (getActivity() == null || !isAdded()) {
                            Log.e("SearchFragment", "Fragment not attached when playing top result");
                            return;
                        }

                        if (!(getActivity() instanceof MainActivity)) {
                            Log.e("SearchFragment", "Activity is not MainActivity when playing top result");
                            return;
                        }

                        // Create single song list and play
                        List<Song> singleSong = Arrays.asList(song);
//                        ((MainActivity) getActivity()).playSong(singleSong, 0);

                        // Gửi intent đến Service để phát nhạc
                        Intent intent = new Intent(requireContext(), MusicService.class);
                        intent.setAction(MusicService.ACTION_PLAY);
                        intent.putExtra("songList", new ArrayList<>(singleSong));
                        intent.putExtra("position", 0);
                        requireContext().startService(intent);

                        // Hiển thị player fragment
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showPlayer(singleSong, 0, "");
                        }

                    } catch (Exception e) {
                        Log.e("SearchFragment", "Error playing top result song: " + e.getMessage(), e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi khi phát nhạc: " + e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e("SearchFragment", "Error in displayTopResultSong: " + e.getMessage(), e);
        }
    }

    private void displayTopResultArtist(Artist artist) {
        tvTopResultTitle.setText(artist.getName());
        tvTopResultSubtitle.setText("Nghệ sĩ");
        tvTopResultType.setText("NGHỆ SĨ");

        Glide.with(this)
                .load(artist.getImageUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(imgTopResultCover);

        imgTopResultPlay.setOnClickListener(v -> {
            // Navigate to artist detail (can be expanded later)
            navigateToArtistDetail(artist);
        });
    }

    private void displaySongs(List<Song> songs) {
        if (songs != null && !songs.isEmpty()) {
            // Show all songs when filtering by songs, otherwise limit to 5
            int limit = (currentFilter == SearchFilter.SONGS) ? songs.size() : Math.min(songs.size(), 5);
            List<Song> limitedSongs = songs.subList(0, limit);

            if (songAdapter == null) {
                songAdapter = new RecyclerViewAdapterSong(getContext(), limitedSongs);
                songAdapter.setOnSongClickListener(this);
                recyclerViewSongs.setAdapter(songAdapter);
            } else {
                songAdapter.updateSongList(limitedSongs);
            }
            layoutSongs.setVisibility(View.VISIBLE);
        } else {
            layoutSongs.setVisibility(View.GONE);
        }
    }

    private void displayArtists(List<Artist> artists) {
        if (artists != null && !artists.isEmpty()) {
            // Show all artists when filtering by artists, otherwise limit to 5
            int limit = (currentFilter == SearchFilter.ARTISTS) ? artists.size() : Math.min(artists.size(), 5);
            List<Artist> limitedArtists = artists.subList(0, limit);

            if (artistAdapter == null) {
                artistAdapter = new SearchArtistAdapter(getContext(), limitedArtists);
                artistAdapter.setOnArtistClickListener(this);
                recyclerViewArtists.setAdapter(artistAdapter);
            } else {
                artistAdapter.updateArtistList(limitedArtists);
            }
            layoutArtists.setVisibility(View.VISIBLE);
        } else {
            layoutArtists.setVisibility(View.GONE);
        }
    }

    private void displayAlbums(List<Album> albums) {
        if (albums != null && !albums.isEmpty()) {
            // Show all albums when filtering by albums, otherwise limit to 5
            int limit = (currentFilter == SearchFilter.ALBUMS) ? albums.size() : Math.min(albums.size(), 5);
            List<Album> limitedAlbums = albums.subList(0, limit);

            if (albumAdapter == null) {
                albumAdapter = new SearchAlbumAdapter(getContext(), limitedAlbums);
                albumAdapter.setOnAlbumClickListener(this);
                recyclerViewAlbums.setAdapter(albumAdapter);
            } else {
                albumAdapter.updateAlbumList(limitedAlbums);
            }
            layoutAlbums.setVisibility(View.VISIBLE);
        } else {
            layoutAlbums.setVisibility(View.GONE);
        }
    }

    private void displayPlaylists(List<Playlist> playlists) {
        if (playlists != null && !playlists.isEmpty()) {
            // Show all playlists when filtering by playlists, otherwise limit to 5
            int limit = (currentFilter == SearchFilter.PLAYLISTS) ? playlists.size() : Math.min(playlists.size(), 5);
            List<Playlist> limitedPlaylists = playlists.subList(0, limit);

            if (playlistAdapter == null) {
                playlistAdapter = new SearchPlaylistAdapter(getContext(), limitedPlaylists);
                playlistAdapter.setOnPlaylistClickListener(this);
                recyclerViewPlaylists.setAdapter(playlistAdapter);
            } else {
                playlistAdapter.updatePlaylistList(limitedPlaylists);
            }
            layoutPlaylists.setVisibility(View.VISIBLE);
        } else {
            layoutPlaylists.setVisibility(View.GONE);
        }
    }

    /**
     * Handle query change for real-time history tracking
     */
    private void handleQueryChange(String newText) {
        // If user is changing from a valid query to a different query
        if (!previousQuery.equals(newText) && isValidQueryForHistory(previousQuery)) {
            // Save previous query to history if it was searched
            if (hasPerformedSearch) {
                saveSearchHistoryIfValid(previousQuery);
            }
        }

        // Update previous query
        previousQuery = currentQuery;
    }

    /**
     * Save previous query to history when appropriate
     */
    private void savePreviousQueryToHistory() {
        if (isValidQueryForHistory(currentQuery) && hasPerformedSearch) {
            saveSearchHistoryIfValid(currentQuery);
        }
        resetSearchState();
    }

    /**
     * Reset search state variables
     */
    private void resetSearchState() {
        previousQuery = "";
        currentQuery = "";
        hasPerformedSearch = false;
    }

    /**
     * Check if query is valid for saving to history
     */
    private boolean isValidQueryForHistory(String query) {
        return query != null &&
                query.trim().length() >= MIN_QUERY_LENGTH_FOR_HISTORY &&
                !query.trim().isEmpty();
    }

    /**
     * Save search history with validation
     */
    private void saveSearchHistoryIfValid(String query) {
        if (!isValidQueryForHistory(query)) {
            return;
        }

        String trimmedQuery = query.trim();

        // Don't save duplicate consecutive searches
        if (!searchHistory.isEmpty() && searchHistory.get(0).equals(trimmedQuery)) {
            return;
        }

        saveSearchHistory(trimmedQuery);
    }

    /**
     * Original save search history method
     */
    private void saveSearchHistory(String query) {
        if (query.trim().isEmpty())
            return;

        // Remove if already exists
        searchHistory.remove(query);

        // Add to the beginning
        searchHistory.add(0, query);

        // Keep only recent searches
        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            searchHistory = searchHistory.subList(0, MAX_HISTORY_SIZE);
        }

        // Save to SharedPreferences
        Gson gson = new Gson();
        String historyJson = gson.toJson(searchHistory);
        searchPreferences.edit().putString(SEARCH_HISTORY_KEY, historyJson).apply();

        // Update adapter real-time
        updateHistoryDisplayRealTime();
    }

    /**
     * Update history display in real-time
     */
    private void updateHistoryDisplayRealTime() {
        if (historyAdapter != null) {
            historyAdapter.updateHistory(searchHistory);
        }

        // Show history if search view is empty and there are items
        if (currentQuery.isEmpty() && !searchHistory.isEmpty()) {
            showSearchHistory();
        }
    }

    private void removeFromHistory(String query) {
        searchHistory.remove(query);

        // Save updated history
        Gson gson = new Gson();
        String historyJson = gson.toJson(searchHistory);
        searchPreferences.edit().putString(SEARCH_HISTORY_KEY, historyJson).apply();

        // Update adapter real-time
        updateHistoryDisplayRealTime();

        // Show/hide history view based on remaining items
        if (searchHistory.isEmpty()) {
            recyclerViewHistory.setVisibility(View.GONE);
        }
    }

    /**
     * Clear all search history (can be called from a menu or long press)
     */
    private void clearAllSearchHistory() {
        if (searchHistory.isEmpty()) {
            return;
        }

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa lịch sử tìm kiếm")
                .setMessage("Bạn có muốn xóa tất cả lịch sử tìm kiếm không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    searchHistory.clear();
                    searchPreferences.edit().remove(SEARCH_HISTORY_KEY).apply();
                    updateHistoryDisplayRealTime();
                    Toast.makeText(getContext(), "Đã xóa tất cả lịch sử tìm kiếm", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showSearchHistory() {
        if (!searchHistory.isEmpty()) {
            recyclerViewHistory.setVisibility(View.VISIBLE);
            if (historyAdapter != null) {
                historyAdapter.updateHistory(searchHistory);
            }
        } else {
            recyclerViewHistory.setVisibility(View.GONE);
        }
    }

    private void hideSearchHistory() {
        recyclerViewHistory.setVisibility(View.GONE);
    }

    private void showSearchSuggestions(String query) {
        // TODO: Implement search suggestions based on popular searches
        recyclerViewSuggestions.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        recyclerViewSuggestions.setVisibility(View.GONE);
    }

    private void showSearchResults() {
        scrollViewResults.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);
    }

    private void hideSearchResults() {
        scrollViewResults.setVisibility(View.GONE);
    }

    private void showNoResults() {
        scrollViewResults.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.VISIBLE);
    }

    private void hideNoResults() {
        tvNoResults.setVisibility(View.GONE);
    }

    @Override
    public void onSongClick(List<Song> songList, int position) {
        try {
            // Validate input parameters
            if (songList == null || songList.isEmpty()) {
                Log.e("SearchFragment", "Song list is null or empty");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Danh sách bài hát trống", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (position < 0 || position >= songList.size()) {
                Log.e("SearchFragment", "Invalid position: " + position + ", list size: " + songList.size());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Vị trí bài hát không hợp lệ", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // Validate activity and context
            if (getActivity() == null || !isAdded()) {
                Log.e("SearchFragment", "Fragment not attached to activity");
                return;
            }

            if (!(getActivity() instanceof MainActivity)) {
                Log.e("SearchFragment", "Activity is not MainActivity");
                return;
            }

            // Validate song data
            Song selectedSong = songList.get(position);
            if (selectedSong == null) {
                Log.e("SearchFragment", "Selected song is null");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Thông tin bài hát không hợp lệ", Toast.LENGTH_SHORT).show();
                }
                return;
            }


            // Gửi intent đến Service để phát nhạc
            Intent intent = new Intent(requireContext(), MusicService.class);
            intent.setAction(MusicService.ACTION_PLAY);
            intent.putExtra("songList", new ArrayList<>(songList));
            intent.putExtra("position", 0);
            requireContext().startService(intent);

            // Hiển thị player fragment
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showPlayer(songList, position, "");
            }

        } catch (Exception e) {
            Log.e("SearchFragment", "Error playing song: " + e.getMessage(), e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi phát nhạc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onHistoryClick(String query) {
        try {
            if (query != null && !query.trim().isEmpty()) {
                // Reset state when clicking history item
                resetSearchState();
                if (searchView != null) {
                    searchView.setQuery(query, false);
                    performSearch(query);
                    hasPerformedSearch = true;
                    currentQuery = query;
                    previousQuery = query;
                }
            }
        } catch (Exception e) {
            Log.e("SearchFragment", "Error handling history click: " + e.getMessage());
        }
    }

    @Override
    public void onHistoryDelete(String query) {
        try {
            if (query != null) {
                removeFromHistory(query);
            }
        } catch (Exception e) {
            Log.e("SearchFragment", "Error deleting history: " + e.getMessage());
        }
    }

    // Navigation methods for search results
    @Override
    public void onArtistClick(Artist artist) {
        try {
            if (artist != null) {
                navigateToArtistDetail(artist);
            }
        } catch (Exception e) {
            Log.e("SearchFragment", "Error navigating to artist: " + e.getMessage());
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi mở nghệ sĩ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAlbumClick(Album album) {
        try {
            if (album != null) {
                navigateToAlbumDetail(album);
            }
        } catch (Exception e) {
            Log.e("SearchFragment", "Error navigating to album: " + e.getMessage());
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi mở album", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        try {
            if (playlist != null) {
                navigateToPlaylistDetail(playlist);
            }
        } catch (Exception e) {
            Log.e("SearchFragment", "Error navigating to playlist: " + e.getMessage());
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi mở playlist", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToArtistDetail(Artist artist) {
        try {
            // For now, show toast - can be expanded to artist detail fragment later
            if (getContext() != null) {
                Toast.makeText(getContext(), "Xem nghệ sĩ: " + artist.getName(), Toast.LENGTH_SHORT).show();
            }

            // TODO: Implement artist detail fragment navigation
            // Example: Navigate to artist albums/songs view
            // ArtistDetailFragment fragment = ArtistDetailFragment.newInstance(artist);
            // getParentFragmentManager().beginTransaction()
            // .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
            // .replace(R.id.fragment_container, fragment)
            // .addToBackStack(null)
            // .commit();
        } catch (Exception e) {
            Log.e("SearchFragment", "Error in navigateToArtistDetail: " + e.getMessage());
        }
    }

    private void navigateToAlbumDetail(Album album) {
        try {
            if (album == null || album.getId() == null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Thông tin album không hợp lệ", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            final String[] artistName = new String[1];
            FirebaseService.getInstance().getArtistNameById(album.getArtistId(), new FirebaseService.FirestoreCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    artistName[0] = result;
                }

                @Override
                public void onError(Exception e) {
                    artistName[0] = "Unknown Artist";
                }
            });
            FirestoreUtils.getSongsByAlbumId(album.getId(), new FirestoreUtils.FirestoreCallback<List<Song>>() {
                @Override
                public void onSuccess(List<Song> songs) {
                    if (!isAdded() || getContext() == null) return;

                    try {
                        AlbumSongsFragment fragment = AlbumSongsFragment.newInstance(
                                songs,
                                album.getTitle(),
                                album.getCoverUrl(),
                                artistName[0]
                        );

                        // Add callback để xử lý khi fragment bị remove
                        fragment.setOnFragmentDismissListener(() -> {
                            try {
                                if (isAdded() && getView() != null) {
                                    View mainContent = getView().findViewById(R.id.mainSearchContent);
                                    View container = getView().findViewById(R.id.albumSongsContainer);
                                    if (mainContent != null && container != null) {
                                        mainContent.setVisibility(View.VISIBLE);
                                        container.setVisibility(View.GONE);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("SearchFragment", "Error in dismiss listener: " + e.getMessage());
                            }
                        });

                        // Ẩn main content và hiện container
                        View mainContent = requireView().findViewById(R.id.mainSearchContent);
                        View container = requireView().findViewById(R.id.albumSongsContainer);
                        if (mainContent != null && container != null) {
                            mainContent.setVisibility(View.GONE);
                            container.setVisibility(View.VISIBLE);
                        }

                        // Thực hiện transaction để thêm fragment mới
                        if (isAdded() && getChildFragmentManager() != null) {
                            getChildFragmentManager().beginTransaction()
                                    .setCustomAnimations(
                                            R.anim.slide_in_right,
                                            R.anim.slide_out_right,
                                            R.anim.slide_in_right,
                                            R.anim.slide_out_right
                                    )
                                    .replace(R.id.albumSongsContainer, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    } catch (Exception e) {
                        Log.e("SearchFragment", "Error in onSuccess: " + e.getMessage());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi khi hiển thị album: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (!isAdded() || getContext() == null) return;

                    Toast.makeText(requireContext(),
                            "Lỗi khi tải danh sách bài hát: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("SearchFragment", "Error in navigateToAlbumDetail: " + e.getMessage());
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi mở album", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToPlaylistDetail(Playlist playlist) {
        try {
            if (playlist == null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Thông tin playlist không hợp lệ", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // Navigate to PlaylistSongsFragment
            PlaylistSongsFragment fragment = PlaylistSongsFragment.newInstance(playlist);

            // Navigate to playlist detail
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToFragment(fragment, "playlist_detail");
            }
        } catch (Exception e) {
            Log.e("SearchFragment", "Error in navigateToPlaylistDetail: " + e.getMessage());
            if (getContext() != null) {
                Toast.makeText(getContext(), "Lỗi khi mở playlist", Toast.LENGTH_SHORT).show();
            }
        }
    }
}