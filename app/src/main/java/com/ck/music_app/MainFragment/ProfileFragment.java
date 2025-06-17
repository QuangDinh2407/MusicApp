package com.ck.music_app.MainFragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.ck.music_app.MainFragment.ProfileChildFragment.DownloadedFragment;
import com.ck.music_app.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private View mainLayout;
    private View fragmentContainer;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mainLayout = view.findViewById(R.id.profile_main_layout);
        fragmentContainer = view.findViewById(R.id.profile_fragment_container);

        DownloadedFragment downloadedFragment = new DownloadedFragment();
        // Load avatar
        ImageView imgAvatar = view.findViewById(R.id.imgAvatar);
        Glide.with(this)
                .load(R.drawable.cat_avatar)
                .circleCrop()
                .into(imgAvatar);

        // Thêm callback để xử lý khi fragment bị remove
        downloadedFragment.setOnFragmentDismissListener(() -> {
            mainLayout.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
        });
        // Xử lý sự kiện click cho các chức năng
        View llDownloaded = view.findViewById(R.id.llDownloaded);
        llDownloaded.setOnClickListener(v -> {
            if (isAdded()) {
                // Ẩn layout chính và hiện container
                mainLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);

                // Thực hiện transaction để thay thế fragment hiện tại bằng DownloadedFragment
                FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                transaction.setCustomAnimations(
                    R.anim.slide_in_left,  // Enter animation
                    R.anim.slide_out_left,  // Exit animation
                    R.anim.slide_in_right,  // Pop enter animation
                    R.anim.slide_out_right  // Pop exit animation
                );
                transaction.replace(R.id.profile_fragment_container, downloadedFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Kiểm tra nếu không có fragment con nào trong stack
        if (getChildFragmentManager().getBackStackEntryCount() == 0) {
            mainLayout.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
        }
    }
}