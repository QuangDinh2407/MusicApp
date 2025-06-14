package com.ck.music_app.MainFragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.ck.music_app.Adapter.ArtistAlbumAdapter;
import com.ck.music_app.MainFragment.HomeChildFragment.AlbumSongsFragment;
import com.ck.music_app.Model.Album;
import com.ck.music_app.Model.Artist;
import com.ck.music_app.Model.ArtistWithAlbums;
import com.ck.music_app.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

        // Cập nhật adapter để xử lý click vào album
        artistAlbumAdapter.setOnAlbumClickListener((album, artist) -> {
            // Tạo và hiển thị AlbumSongsFragment
            AlbumSongsFragment albumSongsFragment = AlbumSongsFragment.newInstance(
                album.getId(),
                album.getTitle(),
                album.getCoverUrl(),
                artist.getName()
            );

            // Thực hiện transaction để thay thế fragment hiện tại bằng AlbumSongsFragment
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, albumSongsFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

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