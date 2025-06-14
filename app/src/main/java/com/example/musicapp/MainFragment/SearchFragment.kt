package com.example.musicapp.MainFragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.Adapter.SearchAdapter
import com.example.musicapp.Model.Song
import com.example.musicapp.R
import com.google.android.material.textfield.TextInputEditText

class SearchFragment : Fragment() {
    private lateinit var searchEditText: TextInputEditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var searchAdapter: SearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        searchEditText = view.findViewById(R.id.searchEditText)
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)

        // Setup RecyclerView
        searchAdapter = SearchAdapter(emptyList()) { song ->
            // Handle song click
            // TODO: Navigate to PlayMusicActivity
        }
        searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }

        // Setup search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch(s.toString())
            }
        })
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            searchAdapter.updateSongs(emptyList())
            showEmptyState(true)
            return
        }

        // TODO: Implement actual search logic
        // For now, we'll just show some dummy data
        val dummyResults = listOf(
            Song("1", "Bài hát 1", "Nghệ sĩ 1", "url1", 100),
            Song("2", "Bài hát 2", "Nghệ sĩ 2", "url2", 200),
            Song("3", "Bài hát 3", "Nghệ sĩ 3", "url3", 300)
        ).filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.artist.contains(query, ignoreCase = true)
        }

        searchAdapter.updateSongs(dummyResults)
        showEmptyState(dummyResults.isEmpty())
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateTextView.visibility = if (show) View.VISIBLE else View.GONE
        searchResultsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
} 