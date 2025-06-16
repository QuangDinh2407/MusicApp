package com.ck.music_app.MainFragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ck.music_app.R;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ImageView imgAvatar = view.findViewById(R.id.imgAvatar);
        // Thay avatar.png bằng đường dẫn ảnh thực tế nếu có
        Glide.with(this)
                .load(R.drawable.cat_avatar)
                .circleCrop()
                .into(imgAvatar);

        // Sự kiện click Đã tải
        View llDownloaded = view.findViewById(R.id.llDownloaded);
        llDownloaded.setOnClickListener(v -> {
            // Ẩn ViewPager2 và hiện fragment container
            View fragmentContainer = requireActivity().findViewById(R.id.fragment_container);
            View viewPager = requireActivity().findViewById(R.id.view_pager);
            fragmentContainer.setVisibility(View.VISIBLE);
            if (viewPager != null) viewPager.setVisibility(View.GONE);

            // Xóa tất cả các fragment trong back stack
            requireActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // Thêm fragment mới
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new DownloadedFragment())
                .addToBackStack(null)
                .commit();
        });

        return view;
    }
}