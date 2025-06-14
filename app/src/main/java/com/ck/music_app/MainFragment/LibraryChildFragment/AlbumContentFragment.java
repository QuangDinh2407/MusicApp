package com.ck.music_app.MainFragment.LibraryChildFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AlbumContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlbumContentFragment extends Fragment {

    private RecyclerView rvAlbum;
    private LinearLayout layoutEmptyState;
    private ChipGroup chipGroupFilter;
    private Chip chipRecent, chipAlphabetical, chipArtist, chipDownloaded;

    public AlbumContentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment AlbumContentFragment.
     */
    public static AlbumContentFragment newInstance() {
        return new AlbumContentFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_content, container, false);
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        return view;
    }

    private void initializeViews(View view) {
        rvAlbum = view.findViewById(R.id.rvAlbum);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipRecent = view.findViewById(R.id.chipRecent);
        chipAlphabetical = view.findViewById(R.id.chipAlphabetical);
        chipArtist = view.findViewById(R.id.chipArtist);
        chipDownloaded = view.findViewById(R.id.chipDownloaded);

        // Set default selection
        chipRecent.setChecked(true);
    }

    private void setupRecyclerView() {
        // Use GridLayoutManager for album grid display
        int spanCount = 2; // Number of columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        rvAlbum.setLayoutManager(layoutManager);
        // TODO: Set adapter and load data
        
        // Temporary: Show empty state
        showEmptyState(true);
    }

    private void setupListeners() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipRecent) {
                // TODO: Sort by recent
            } else if (checkedId == R.id.chipAlphabetical) {
                // TODO: Sort alphabetically
            } else if (checkedId == R.id.chipArtist) {
                // TODO: Sort by artist
            } else if (checkedId == R.id.chipDownloaded) {
                // TODO: Filter downloaded albums
            }
        });
    }

    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvAlbum.setVisibility(show ? View.GONE : View.VISIBLE);
    }
} 