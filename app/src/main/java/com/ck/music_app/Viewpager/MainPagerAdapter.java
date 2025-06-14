package com.ck.music_app.Viewpager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {
    private Fragment[] fragments;

    public MainPagerAdapter(@NonNull FragmentActivity fa, Fragment[] fragments) {
        super(fa);
        this.fragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return empty fragment if fragments not initialized yet
        if (fragments == null || position >= fragments.length || fragments[position] == null) {
            return new Fragment(); // Empty placeholder fragment
        }
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments != null ? fragments.length : 4; // Default to 4 tabs
    }

    public void updateFragments(Fragment[] newFragments) {
        this.fragments = newFragments;
        notifyDataSetChanged();
    }
}
