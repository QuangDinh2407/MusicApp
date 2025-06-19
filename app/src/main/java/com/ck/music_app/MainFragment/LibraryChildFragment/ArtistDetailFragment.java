package com.ck.music_app.MainFragment.LibraryChildFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.ck.music_app.Model.Artist;
import com.ck.music_app.R;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class ArtistDetailFragment extends Fragment {
    private Artist artist;
    private ImageView imgArtist, imgBackground;
    private ImageButton btnBack;
    private MaterialButton btnFollow;
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private TextView tvArtistName;
    private TextView tvBio;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private boolean isFollowing = false;
    private OnFragmentDismissListener dismissListener;

    public interface OnFragmentDismissListener {
        void onDismiss();
    }

    public void setOnFragmentDismissListener(OnFragmentDismissListener listener) {
        this.dismissListener = listener;
    }

    public static ArtistDetailFragment newInstance(Artist artist) {
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        Bundle args = new Bundle();
        args.putString("artistId", artist.getId());
        args.putString("artistName", artist.getName());
        args.putString("artistImageUrl", artist.getImageUrl());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_detail, container, false);
        initializeViews(view);
        setupFirebase();
        loadArtistDetails();
        checkFollowingStatus();
        return view;
    }

    private void initializeViews(View view) {
        imgArtist = view.findViewById(R.id.imgArtist);
        imgBackground = view.findViewById(R.id.imgBackground);
        btnBack = view.findViewById(R.id.btnBack);
        btnFollow = view.findViewById(R.id.btnFollow);
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar);
        appBarLayout = view.findViewById(R.id.appBarLayout);
        tvArtistName = view.findViewById(R.id.tvArtistName);
        tvBio = view.findViewById(R.id.tvBio);

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnFollow.setOnClickListener(v -> toggleFollow());
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void loadArtistDetails() {
        if (getArguments() == null) return;

        String artistId = getArguments().getString("artistId");
        String artistName = getArguments().getString("artistName");
        String artistImageUrl = getArguments().getString("artistImageUrl");

        // Create artist object
        artist = new Artist(artistId, artistName, artistImageUrl);

        // Update UI
        tvArtistName.setText(artistName);
        tvBio.setText("Ca sĩ Việt Nam nổi tiếng"); // You can update this with actual bio from Firebase

        // Load artist image
        if (artistImageUrl != null && !artistImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(artistImageUrl)
                    .placeholder(R.drawable.default_artist)
                    .error(R.drawable.default_artist)
                    .circleCrop()
                    .into(imgArtist);

            // Load background image with blur effect
            Glide.with(this)
                    .load(artistImageUrl)
                    .placeholder(R.drawable.default_artist)
                    .error(R.drawable.default_artist)
                    .into(imgBackground);
        }
    }

    private void checkFollowingStatus() {
        if (currentUser == null || artist == null || !isAdded() || getContext() == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .collection("followedArtists")
                .document(artist.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || getContext() == null) return;
                    isFollowing = documentSnapshot.exists();
                    updateFollowButton();
                });
    }

    private void toggleFollow() {
        if (currentUser == null || artist == null || !isAdded() || getContext() == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập để theo dõi nghệ sĩ", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userDoc = db.collection("users").document(currentUser.getUid());
        DocumentReference artistDoc = db.collection("artists").document(artist.getId());

        if (!isFollowing) {
            // Follow artist
            userDoc.collection("followedArtists")
                    .document(artist.getId())
                    .set(artist)
                    .addOnSuccessListener(aVoid -> {
                        if (!isAdded() || getContext() == null) return;
                        artistDoc.update("followerCount", FieldValue.increment(1));
                        isFollowing = true;
                        updateFollowButton();
                        Toast.makeText(getContext(), "Đã theo dõi " + artist.getName(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Lỗi khi theo dõi nghệ sĩ", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Unfollow artist
            userDoc.collection("followedArtists")
                    .document(artist.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        if (!isAdded() || getContext() == null) return;
                        artistDoc.update("followerCount", FieldValue.increment(-1));
                        isFollowing = false;
                        updateFollowButton();
                        Toast.makeText(getContext(), "Đã hủy theo dõi " + artist.getName(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Lỗi khi hủy theo dõi nghệ sĩ", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnFollow.setText("Đang theo dõi");
            btnFollow.setStrokeWidth(0);
            btnFollow.setBackgroundColor(requireContext().getColor(R.color.flatGreenEnd));
        } else {
            btnFollow.setText("Theo dõi");
            btnFollow.setStrokeWidth(requireContext().getResources()
                    .getDimensionPixelSize(R.dimen.button_stroke_width));
            btnFollow.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }
} 