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
import com.ck.music_app.Model.Song;
import com.ck.music_app.R;
import com.ck.music_app.Services.FirebaseService;
import com.ck.music_app.utils.FirestoreUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ListView listViewArtists;
    private View homeFragmentContainer;
    private List<ArtistWithAlbums> artistWithAlbumsList = new ArrayList<>();
    private ArtistAlbumAdapter artistAlbumAdapter;
    private FirebaseService firebaseService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Khởi tạo FirebaseService sử dụng getInstance()
        firebaseService = FirebaseService.getInstance();

        // Khởi tạo views
        listViewArtists = view.findViewById(R.id.listViewArtists);
        homeFragmentContainer = view.findViewById(R.id.home_fragment_container);

        // Khởi tạo adapter cho danh sách nghệ sĩ
        artistAlbumAdapter = new ArtistAlbumAdapter(getContext(), artistWithAlbumsList);
        listViewArtists.setAdapter(artistAlbumAdapter);

        // Load dữ liệu nghệ sĩ và album
        loadArtistsAndAlbums();

        // Cập nhật adapter để xử lý click vào album
        artistAlbumAdapter.setOnAlbumClickListener((album, artist) -> {
            // Lấy danh sách bài hát từ album
            FirestoreUtils.getSongsByAlbumId(album.getId(), new FirestoreUtils.FirestoreCallback<List<Song>>() {
                @Override
                public void onSuccess(List<Song> songs) {
                    // Tạo và hiển thị AlbumSongsFragment với danh sách bài hát
                    AlbumSongsFragment albumSongsFragment = AlbumSongsFragment.newInstance(
                        songs,
                        album.getTitle(),
                        album.getCoverUrl(),
                        artist.getName()
                    );

                    // Thêm callback để xử lý khi fragment bị remove
                    albumSongsFragment.setOnFragmentDismissListener(() -> {
                        listViewArtists.setVisibility(View.VISIBLE);
                        homeFragmentContainer.setVisibility(View.GONE);
                    });

                    // Thực hiện transaction để thay thế fragment hiện tại bằng AlbumSongsFragment
                    if (isAdded()) {
                        // Ẩn ListView và hiện container
                        listViewArtists.setVisibility(View.GONE);
                        homeFragmentContainer.setVisibility(View.VISIBLE);

                        // Sử dụng childFragmentManager thay vì parentFragmentManager
                        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                        transaction.setCustomAnimations(
                                R.anim.slide_in_left, // Enter animation
                                R.anim.slide_out_left, // Exit animation
                                R.anim.slide_in_right, // Pop enter animation
                                R.anim.slide_out_right // Pop exit animation
                        );
                        transaction.replace(R.id.home_fragment_container, albumSongsFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Xử lý lỗi nếu cần
                }
            });
        });

        loadArtistsAndAlbums();

        return view;
    }

    private void loadArtistsAndAlbums() {
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
    }

    @Override
    public void onResume() {
        super.onResume();
        // Kiểm tra nếu không có fragment con nào trong stack
        if (getChildFragmentManager().getBackStackEntryCount() == 0) {
            listViewArtists.setVisibility(View.VISIBLE);
            homeFragmentContainer.setVisibility(View.GONE);
        }
    }
}