package com.ck.music_app.MainFragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.util.Log;

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
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
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
                    if (!isAdded()) return;

                    // Tạo và hiển thị AlbumSongsFragment với danh sách bài hát
                    AlbumSongsFragment albumSongsFragment = AlbumSongsFragment.newInstance(
                        songs,
                        album.getTitle(),
                        album.getCoverUrl(),
                        artist.getName()
                    );

                    // Thêm callback để xử lý khi fragment bị remove
                    albumSongsFragment.setOnFragmentDismissListener(() -> {
                        if (isAdded()) {
                            listViewArtists.setVisibility(View.VISIBLE);
                            homeFragmentContainer.setVisibility(View.GONE);
                        }
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
                    if (isAdded() && getContext() != null) {
                        Log.e(TAG, "Error loading songs: " + e.getMessage());
                    }
                }
            });
        });

        return view;
    }

    private void loadArtistsAndAlbums() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Clear list before loading new data
        artistWithAlbumsList.clear();
        artistAlbumAdapter.notifyDataSetChanged();

        db.collection("artists").get().addOnSuccessListener(artistSnapshots -> {
            if (!isAdded()) return;

            List<Artist> artists = new ArrayList<>();
            for (QueryDocumentSnapshot artistDoc : artistSnapshots) {
                Artist artist = artistDoc.toObject(Artist.class);
                artist.setId(artistDoc.getId());
                artists.add(artist);
            }

            // Sử dụng AtomicInteger để theo dõi số lượng artist đã được xử lý
            AtomicInteger processedArtists = new AtomicInteger(0);
            int totalArtists = artists.size();

            for (Artist artist : artists) {
                // Lấy album của nghệ sĩ này
                db.collection("albums")
                    .whereEqualTo("artistId", artist.getId())
                    .get()
                    .addOnSuccessListener(albumSnapshots -> {
                        if (!isAdded()) return;

                        List<Album> albums = new ArrayList<>();
                        for (QueryDocumentSnapshot albumDoc : albumSnapshots) {
                            Album album = albumDoc.toObject(Album.class);
                            album.setId(albumDoc.getId());
                            albums.add(album);
                        }

                        // Thêm nghệ sĩ và album vào list
                        artistWithAlbumsList.add(new ArtistWithAlbums(artist, albums));

                        // Kiểm tra nếu đã xử lý hết tất cả nghệ sĩ
                        if (processedArtists.incrementAndGet() == totalArtists) {
                            artistAlbumAdapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded() && getContext() != null) {
                            Log.e(TAG, "Error loading albums for artist " + artist.getName() + ": " + e.getMessage());
                        }
                        // Vẫn tăng counter ngay cả khi thất bại để không bị treo
                        if (processedArtists.incrementAndGet() == totalArtists) {
                            artistAlbumAdapter.notifyDataSetChanged();
                        }
                    });
            }
        }).addOnFailureListener(e -> {
            if (isAdded() && getContext() != null) {
                Log.e(TAG, "Error loading artists: " + e.getMessage());
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