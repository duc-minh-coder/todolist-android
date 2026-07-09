package com.baitaplon.todo_list.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.baitaplon.todo_list.fragment.NotesFragment;
import com.baitaplon.todo_list.fragment.ScheduleFragment;

public class MainViewPagerAdapter extends FragmentStateAdapter {
    public MainViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new NotesFragment();
        }
        return new ScheduleFragment(); //p = 0
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}