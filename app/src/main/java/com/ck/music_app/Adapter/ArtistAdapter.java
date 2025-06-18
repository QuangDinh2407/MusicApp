package com.ck.music_app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.Artist;
import com.ck.music_app.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    private List<Artist> artists;
    private final OnArtistClickListener listener;
    private List<String> followedArtistIds;

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
        void onFollowClick(Artist artist, boolean isFollowing);
    }

    public ArtistAdapter(List<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.listener = listener;
    }

    public void updateData(List<Artist> newArtists) {
        this.artists = newArtists;
        notifyDataSetChanged();
    }

    public void updateFollowedArtists(List<String> followedArtistIds) {
        this.followedArtistIds = followedArtistIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artists.get(position);
        holder.bind(artist);
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    class ArtistViewHolder extends RecyclerView.ViewHolder {
        private final View layoutArtistInfo;
        private final ImageView imgArtist;
        private final TextView tvArtistName;
        private final MaterialButton btnFollow;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutArtistInfo = itemView.findViewById(R.id.layoutArtistInfo);
            imgArtist = itemView.findViewById(R.id.imgArtist);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }

        public void bind(Artist artist) {
            tvArtistName.setText(artist.getName());

            // Load artist image using Glide
            if (artist.getImageUrl() != null && !artist.getImageUrl().isEmpty()) {
                Glide.with(imgArtist.getContext())
                        .load(artist.getImageUrl())
                        .placeholder(R.drawable.default_artist)
                        .error(R.drawable.default_artist)
                        .circleCrop()
                        .into(imgArtist);
            } else {
                imgArtist.setImageResource(R.drawable.default_artist);
            }

            // Set up click listeners
            layoutArtistInfo.setOnClickListener(v -> listener.onArtistClick(artist));

            // Update follow button state
            boolean isFollowing = followedArtistIds != null && 
                                followedArtistIds.contains(artist.getId());
            updateFollowButton(btnFollow, isFollowing);

            btnFollow.setOnClickListener(v -> {
                boolean newFollowState = !isFollowing;
                updateFollowButton(btnFollow, newFollowState);
                listener.onFollowClick(artist, newFollowState);
            });
        }

        private void updateFollowButton(MaterialButton button, boolean isFollowing) {
            if (isFollowing) {
                button.setText("Đang theo dõi");
                button.setStrokeWidth(0);
                button.setBackgroundColor(button.getContext().getColor(R.color.flatGreenEnd));
            } else {
                button.setText("Theo dõi");
                button.setStrokeWidth(button.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.button_stroke_width));
                button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
    }
} 