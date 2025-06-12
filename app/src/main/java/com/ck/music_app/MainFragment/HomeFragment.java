package com.ck.music_app.MainFragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ck.music_app.Adapter.ArtistAlbumAdapter;
import com.ck.music_app.MainActivity;
import com.ck.music_app.Model.Album;
import com.ck.music_app.Model.Artist;
import com.ck.music_app.Model.ArtistWithAlbums;
import com.ck.music_app.Model.Song;
import com.ck.music_app.PlayMusicActivity;
import com.ck.music_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ListView listViewArtists;
    private List<ArtistWithAlbums> artistWithAlbumsList = new ArrayList<>();
    private ArtistAlbumAdapter artistAlbumAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        listViewArtists = view.findViewById(R.id.listViewArtists);
        artistAlbumAdapter = new ArtistAlbumAdapter(getContext(), artistWithAlbumsList);
        listViewArtists.setAdapter(artistAlbumAdapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("artists").get().addOnSuccessListener(artistSnapshots -> {
            artistWithAlbumsList.clear();
            for (QueryDocumentSnapshot artistDoc : artistSnapshots) {
                Artist artist = artistDoc.toObject(Artist.class);
                artist.setId(artistDoc.getId());

                // Lấy album của nghệ sĩ này
                db.collection("albums").whereEqualTo("artistId", artist.getId()).get().addOnSuccessListener(albumSnapshots -> {
                    List<Album> albums = new ArrayList<>();
                    for (QueryDocumentSnapshot albumDoc : albumSnapshots) {
                        Album album = albumDoc.toObject(Album.class);
                        album.setId(albumDoc.getId());
                        albums.add(album);
                    }
                    artistWithAlbumsList.add(new ArtistWithAlbums(artist, albums));
                    artistAlbumAdapter.notifyDataSetChanged();
                });
            }
        });

        return view;
    }
}