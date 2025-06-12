package com.ck.music_app.MainFragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.ck.music_app.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LibraryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LibraryFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public LibraryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LibaryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LibraryFragment newInstance(String param1, String param2) {
        LibraryFragment fragment = new LibraryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabAdd;
    private FirebaseAuth mAuth; // Giữ lại FirebaseAuth để kiểm tra trạng thái đăng nhập cho FAB

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPagerLibrary);
        fabAdd = view.findViewById(R.id.fabAddPlaylist);

        // Set up ViewPager2 with a custom adapter
        LibraryPagerAdapter pagerAdapter = new LibraryPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Playlist");
                    break;
                case 1:
                    tab.setText("Nghệ sĩ");
                    break;
                case 2:
                    tab.setText("Album");
                    break;
            }
        }).attach();

        fabAdd.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm nội dung", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentTab = viewPager.getCurrentItem();
            if (currentTab == 0) { // Playlist tab selected
                // Trigger showAddPlaylistDialog in PlaylistContentFragment
                Fragment currentFragment = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
                if (currentFragment instanceof PlaylistContentFragment) {
                    ((PlaylistContentFragment) currentFragment).showAddPlaylistDialog();
                } else {
                    // This might happen if fragment is not yet initialized or recreated
                    Toast.makeText(getContext(), "Fragment Playlist chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
                }
            } else if (currentTab == 1) { // Album tab selected
                Toast.makeText(getContext(), "Chức năng thêm Album sẽ được triển khai sau", Toast.LENGTH_SHORT).show();
                // TODO: Show add album dialog
            }
        });

        return view;
    }

    // Adapter for ViewPager2
    private static class LibraryPagerAdapter extends FragmentStateAdapter {
        public LibraryPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return PlaylistContentFragment.newInstance("", "");
                case 1:
                    return ArtistContentFragment.newInstance("", "");
                case 2:
                    return AlbumContentFragment.newInstance("", "");
                default:
                    return PlaylistContentFragment.newInstance("", "");
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Number of tabs (Playlist, Nghệ sĩ, Album)
        }
    }
}