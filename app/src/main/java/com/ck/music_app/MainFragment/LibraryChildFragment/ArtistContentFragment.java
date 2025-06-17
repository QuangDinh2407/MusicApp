package com.ck.music_app.MainFragment.LibraryChildFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ck.music_app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ArtistContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ArtistContentFragment extends Fragment {

    private RecyclerView rvFollowedArtists;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutSearchArtist;
    private ChipGroup chipGroupFilter;
    private Chip chipRecent, chipAlphabetical;

    public ArtistContentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment ArtistContentFragment.
     */
    public static ArtistContentFragment newInstance() {
        return new ArtistContentFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_content, container, false);
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        return view;
    }

    private void initializeViews(View view) {
        rvFollowedArtists = view.findViewById(R.id.rvFollowedArtists);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        layoutSearchArtist = view.findViewById(R.id.layoutSearchArtist);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipRecent = view.findViewById(R.id.chipRecent);
        chipAlphabetical = view.findViewById(R.id.chipAlphabetical);

        // Set default selection
        chipRecent.setChecked(true);
    }

    private void setupRecyclerView() {
        rvFollowedArtists.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Set adapter and load data
        
        // Temporary: Show empty state
        showEmptyState(true);
    }

    private void setupListeners() {
        layoutSearchArtist.setOnClickListener(v -> {
            // TODO: Implement artist search
            Toast.makeText(getContext(), "Tính năng tìm kiếm nghệ sĩ sẽ được triển khai sau", Toast.LENGTH_SHORT).show();
        });

        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipRecent) {
                // TODO: Sort by recent
            } else if (checkedId == R.id.chipAlphabetical) {
                // TODO: Sort alphabetically
            }
        });
    }

    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFollowedArtists.setVisibility(show ? View.GONE : View.VISIBLE);
    }
} 