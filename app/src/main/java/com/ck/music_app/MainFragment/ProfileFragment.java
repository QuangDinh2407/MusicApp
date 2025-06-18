package com.ck.music_app.MainFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ck.music_app.MainFragment.ProfileChildFragment.DownloadedFragment;
import com.ck.music_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private View mainLayout;
    private View fragmentContainer;

    private ImageView imgAvatar;

    private TextView tvUserName;
    private LocalBroadcastManager broadcaster;
    private DownloadedFragment downloadedFragment;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("OPEN_DOWNLOAD_FRAGMENT".equals(intent.getAction())) {
                openDownloadFragment();
            }
        }
    };

    private FirebaseAuth auth;
    private FirebaseFirestore db;

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
        broadcaster = LocalBroadcastManager.getInstance(requireContext());
        broadcaster.registerReceiver(downloadReceiver, new IntentFilter("OPEN_DOWNLOAD_FRAGMENT"));
        
        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mainLayout = view.findViewById(R.id.profile_main_layout);
        fragmentContainer = view.findViewById(R.id.profile_fragment_container);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);

        downloadedFragment = new DownloadedFragment();

        // Load thông tin người dùng từ Firebase
        loadUserInfo();

        // Thêm callback để xử lý khi fragment bị remove
        downloadedFragment.setOnFragmentDismissListener(() -> {
            mainLayout.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
        });

        // Xử lý sự kiện click cho các chức năng
        View llDownloaded = view.findViewById(R.id.llDownloaded);
        llDownloaded.setOnClickListener(v -> openDownloadFragment());

        // Kiểm tra xem có yêu cầu mở download fragment không
        if (getActivity() != null && getActivity().getIntent() != null) {
            boolean shouldOpenDownload = getActivity().getIntent().getBooleanExtra("openDownloadFragment", false);
            if (shouldOpenDownload) {
                // Delay một chút để đảm bảo fragment đã được tạo hoàn toàn
                view.post(() -> openDownloadFragment());
            }
        }

        return view;
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {

            // Lấy thông tin từ Firestore
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Lấy tên người dùng từ Firestore
                        String name = documentSnapshot.getString("Name");
                        if (name != null && !name.isEmpty()) {
                            tvUserName.setText(name);
                        } else {
                            // Nếu không có tên trong Firestore, dùng email từ Firebase Auth
                            String email = currentUser.getEmail();
                            tvUserName.setText(email != null ? email : "Unknown User");
                        }

                        // Load avatar
                        String photoUrl = currentUser.getPhotoUrl() != null ? 
                            currentUser.getPhotoUrl().toString() : null;
                            
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            // Nếu có avatar từ social login (Google/Facebook)
                            Glide.with(ProfileFragment.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.cat_avatar)
                                .error(R.drawable.cat_avatar)
                                .circleCrop()
                                .into(imgAvatar);
                        } else {
                            // Nếu không có avatar, load ảnh mặc định
                            Glide.with(ProfileFragment.this)
                                .load(R.drawable.cat_avatar)
                                .circleCrop()
                                .into(imgAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), 
                        "Failed to load user info: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        } else {
            // Nếu chưa đăng nhập, hiển thị thông tin mặc định
            tvUserName.setText("Guest User");
            Glide.with(this)
                .load(R.drawable.cat_avatar)
                .circleCrop()
                .into(imgAvatar);
        }
    }

    private void openDownloadFragment() {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        broadcaster.unregisterReceiver(downloadReceiver);
    }
}