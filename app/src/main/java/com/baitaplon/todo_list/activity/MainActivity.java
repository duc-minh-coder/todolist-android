package com.baitaplon.todo_list.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.adapter.MainViewPagerAdapter;
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.fragment.AddEditNoteFragment;
import com.baitaplon.todo_list.fragment.AddEditScheduleFragment;
import com.baitaplon.todo_list.fragment.SettingFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tabSchedule;
    private TextView tabNotes;
    private ImageView btnNotifications;
    private ImageView btnMore;
    private FloatingActionButton fabAdd;
    private FloatingActionButton fabClose;
    private FloatingActionButton fabSchedule;
    private FloatingActionButton fabNote;
    private TextView tvNotificationBadge;
    private MainViewPagerAdapter adapter;
    private LocalDataStore localDataStore;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        localDataStore = LocalDataStore.getInstance(this);
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);
        if (currentUserId == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        initUI();
        adapter = new MainViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        tabSchedule.setOnClickListener(v -> viewPager.setCurrentItem(0));
        tabNotes.setOnClickListener(v -> viewPager.setCurrentItem(1));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabs(position);
            }
        });

        fabAdd.setOnClickListener(v -> toggleFabMenu());
        fabClose.setOnClickListener(v -> closeFabMenu());
        fabNote.setOnClickListener(v -> { showAddNoteFragment(); closeFabMenu(); });
        fabSchedule.setOnClickListener(v -> { showAddScheduleFragment(); closeFabMenu(); });
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NotificationsActivity.class)));
        btnMore.setOnClickListener(v -> showSettings());

        updateTabs(0);
        updateNotificationBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        int unreadCount = localDataStore.notifications().getUnreadByUser(currentUserId).size();
        if (tvNotificationBadge == null) return;
        if (unreadCount > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void showSettings() {
        FrameLayout mainContainer = findViewById(R.id.main_container);
        RelativeLayout topBar = findViewById(R.id.top_bar);
        viewPager.setVisibility(View.GONE);
        fabAdd.setVisibility(View.GONE);
        topBar.setVisibility(View.GONE);
        mainContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new SettingFragment()).addToBackStack(null).commit();
    }

    private void showAddNoteFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(android.R.id.content, new AddEditNoteFragment(), "AddNoteFragment");
        ft.addToBackStack(null);
        ft.commit();
    }

    private void showAddScheduleFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(android.R.id.content, new AddEditScheduleFragment(), "AddScheduleFragment");
        ft.addToBackStack(null);
        ft.commit();
    }

    private void updateTabs(int position) {
        boolean isScheduleTab = position == 0;
        tabSchedule.setBackgroundResource(isScheduleTab ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tabSchedule.setTextColor(ContextCompat.getColor(this, isScheduleTab ? R.color.primary : R.color.white));
        tabNotes.setBackgroundResource(isScheduleTab ? R.drawable.bg_tab_unselected : R.drawable.bg_tab_selected);
        tabNotes.setTextColor(ContextCompat.getColor(this, isScheduleTab ? R.color.white : R.color.primary));
    }

    private void initUI() {
        viewPager = findViewById(R.id.view_pager);
        tabSchedule = findViewById(R.id.tab_schedule);
        tabNotes = findViewById(R.id.tab_notes);
        fabAdd = findViewById(R.id.fab_add);
        fabClose = findViewById(R.id.fab_close);
        fabSchedule = findViewById(R.id.fab_schedule);
        fabNote = findViewById(R.id.fab_note);
        btnNotifications = findViewById(R.id.btn_notifications);
        btnMore = findViewById(R.id.btn_Caidat);
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);
    }

    private void toggleFabMenu() {
        fabAdd.setVisibility(View.GONE);
        fabClose.setVisibility(View.VISIBLE);
        fabSchedule.setVisibility(View.VISIBLE);
        fabNote.setVisibility(View.VISIBLE);
    }

    private void closeFabMenu() {
        fabClose.setVisibility(View.GONE);
        fabSchedule.setVisibility(View.GONE);
        fabNote.setVisibility(View.GONE);
        fabAdd.setVisibility(View.VISIBLE);
    }
}