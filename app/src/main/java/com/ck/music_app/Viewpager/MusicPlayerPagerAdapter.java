package com.ck.music_app.Viewpager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MusicPlayerPagerAdapter extends FragmentStateAdapter {
    private final Fragment[] fragments;

    public MusicPlayerPagerAdapter(@NonNull FragmentActivity fa, Fragment[] fragments) {
        super(fa);
        this.fragments = fragments;
    }

    public MusicPlayerPagerAdapter(@NonNull Fragment fragment, Fragment[] fragments) {
        super(fragment);
        this.fragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments.length;
    }
}
