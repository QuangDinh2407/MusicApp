package com.ck.music_app.MainFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ck.music_app.Auth.LoginActivity;
import com.ck.music_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

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

    private FirebaseAuth auth;
    private MaterialButton logoutButton;

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
        System.out.println("HAHA");
        System.out.printf(String.valueOf(R.layout.fragment_profile));
        // Khởi tạo Firebase Auth
        auth = FirebaseAuth.getInstance();
        
        // Khởi tạo nút đăng xuất
        logoutButton = view.findViewById(R.id.logoutButton);
        
        // Thiết lập sự kiện click cho nút đăng xuất
        logoutButton.setOnClickListener(v -> logout());
        
        return view;
    }

    private void logout() {
        // Hiển thị dialog xác nhận đăng xuất
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất", (dialog, which) -> {
                // Đăng xuất khỏi Firebase
                auth.signOut();
                
                // Xóa dữ liệu trong cả hai SharedPreferences
                SharedPreferences loginSession = requireContext().getSharedPreferences("LoginSession", Context.MODE_PRIVATE);
                SharedPreferences loginPrefs = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                
                loginSession.edit().clear().apply();
                loginPrefs.edit().clear().apply();
                
                // Chuyển về màn hình đăng nhập
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}