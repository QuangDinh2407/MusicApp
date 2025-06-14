package com.example.musicapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.Model.Song
import com.example.musicapp.R

class SearchAdapter(
    private var songs: List<Song>,
    private val onItemClick: (Song) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView = itemView.findViewById(R.id.imgCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvViewCount: TextView = itemView.findViewById(R.id.tvViewCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val song = songs[position]
        holder.tvTitle.text = song.title
        holder.tvViewCount.text = "Lượt nghe: ${song.viewCount}"
        // TODO: Load image using Glide or Coil
        holder.itemView.setOnClickListener { onItemClick(song) }
    }

    override fun getItemCount() = songs.size

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }
} 