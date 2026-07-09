package com.baitaplon.todo_list.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.adapter.MainViewPagerAdapter;
import com.baitaplon.todo_list.fragment.AddEditNoteFragment;
import com.baitaplon.todo_list.fragment.AddEditScheduleFragment;
import com.baitaplon.todo_list.fragment.SettingFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {
    ViewPager2 viewPager;
    TextView tabSchedule, tabNotes;
    ImageView btnNotifications, btnMore;
    FloatingActionButton fabAdd, fabClose, fabSchedule, fabNote;
    TextView tvFabSchedule, tvFabNote;
    MainViewPagerAdapter adapter;
    boolean isFabMenuOpen = false;

    private TextView tvNotificationBadge;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;
    private String currentUserId;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lấy user_id đã lưu từ SharedPreferences
        SharedPreferences sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);
        if(currentUserId == null){
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            this.finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        initUI();
        adapter = new MainViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabs(position);
            }
        });

        tabSchedule.setOnClickListener(v -> viewPager.setCurrentItem(0));
        tabNotes.setOnClickListener(v -> viewPager.setCurrentItem(1));

        updateTabs(0);

        fabAdd.setOnClickListener(v -> showFabMenu());
        fabClose.setOnClickListener(v -> closeFabMenu());

        fabNote.setOnClickListener(v-> {
            showAddNoteFragment();
            closeFabMenu();
        });
        fabSchedule.setOnClickListener(v->{
            showAddScheduleFragment();
            closeFabMenu();
        });

        btnNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                startActivity(intent);
            }
        });

        listenForUnreadNotifications();

        btnMore.setOnClickListener(v -> {
//            new AlertDialog.Builder(this)
//                    .setTitle("Đăng xuất")
//                    .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này?")
//                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
//                        logoutUser();
//                    })
//                    .setNegativeButton("Hủy", (dialog, which) -> {
//                        dialog.dismiss();
//                    })
//                    .show();
        });
        FrameLayout mainContainer = findViewById(R.id.main_container);
        RelativeLayout topBar = findViewById(R.id.top_bar);
        // Ẩn container setting ban đầu
        mainContainer.setVisibility(View.GONE);
        btnMore.setOnClickListener(v->{
            // Ẩn ViewPager và hiện container để load fragment Setting
            viewPager.setVisibility(View.GONE);
            mainContainer.setVisibility(View.VISIBLE);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
            ft.replace(R.id.main_container, new SettingFragment());
            ft.addToBackStack(null);
            ft.commit();
        });

        btnMore.setOnClickListener(v -> {
            // Ẩn các thành phần khác
            viewPager.setVisibility(View.GONE);
            fabAdd.setVisibility(View.GONE);
            topBar.setVisibility(View.GONE);

            // Hiện SettingFragment
            mainContainer.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new SettingFragment())
                    .addToBackStack(null)
                    .commit();
        });


    }

    private void listenForUnreadNotifications() {
        if (currentUserId == null) return;

        if (notificationListener != null) {
            notificationListener.remove();
        }

        // query: đếm số thông báo của user này và chưa đọc (isRead == false)
        Query query = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false);

        notificationListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen for notifications failed.", e);
                return;
            }

            if (snapshots != null) {
                int unreadCount = snapshots.size(); // Lấy số lượng document
                Log.d(TAG, "Unread notifications count: " + unreadCount);
                // Cập nhật giao diện trên UI Thread
                runOnUiThread(() -> updateNotificationBadge(unreadCount));
            } else {
                Log.d(TAG, "Current notification data: null");
                runOnUiThread(() -> updateNotificationBadge(0));
            }
        });
    }

    private void updateNotificationBadge(int count) {
        if (tvNotificationBadge == null) return;

        if (count > 0) {
            if (count > 9) {
                tvNotificationBadge.setText("9+");
            } else {
                tvNotificationBadge.setText(String.valueOf(count));
            }
            tvNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void logoutUser() {
        if (notificationListener != null) {
            notificationListener.remove();
        }

        FirebaseAuth.getInstance().signOut();

        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("current_user_id");
        editor.remove("current_user_name");
        editor.apply();

        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAddNoteFragment() {
        AddEditNoteFragment addNoteFragment = new AddEditNoteFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.add(android.R.id.content, addNoteFragment, "AddNoteFragment");
        ft.addToBackStack(null);
        ft.commit();
    }

    private void showAddScheduleFragment(){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        //content: cả màn hình activity này
        fragmentTransaction.add(android.R.id.content, new AddEditScheduleFragment(), "AddScheduleFragment");
        fragmentTransaction.addToBackStack(null); //cho phép back lại
        fragmentTransaction.commit();
    }

    private void updateTabs(int position) {
        if (position == 0) {
            // Tab Lịch trình (Được chọn)
            tabSchedule.setBackgroundResource(R.drawable.bg_tab_selected);
            tabSchedule.setTextColor(ContextCompat.getColor(this, R.color.primary));

            // Tab Ghi chú (Không được chọn)
            tabNotes.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabNotes.setTextColor(ContextCompat.getColor(this, R.color.white));

        } else {
            // Tab Lịch trình (Không được chọn)
            tabSchedule.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabSchedule.setTextColor(ContextCompat.getColor(this, R.color.white));

            // Tab Ghi chú (Được chọn)
            tabNotes.setBackgroundResource(R.drawable.bg_tab_selected);
            tabNotes.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void initUI(){
        viewPager = findViewById(R.id.view_pager);
        tabSchedule = findViewById(R.id.tab_schedule);
        tabNotes = findViewById(R.id.tab_notes);

        fabAdd = findViewById(R.id.fab_add);
        fabClose = findViewById(R.id.fab_close);
        fabSchedule = findViewById(R.id.fab_schedule);
        fabNote = findViewById(R.id.fab_note);
        tvFabSchedule = findViewById(R.id.tv_fab_schedule);
        tvFabNote = findViewById(R.id.tv_fab_note);
        btnNotifications = findViewById(R.id.btn_notifications);
        btnMore = findViewById(R.id.btn_Caidat);
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    private void showFabMenu() {
        isFabMenuOpen = true;
        fabAdd.setVisibility(View.GONE);
        fabClose.setVisibility(View.VISIBLE);

        fabSchedule.setVisibility(View.VISIBLE);
        tvFabSchedule.setVisibility(View.VISIBLE);
        fabNote.setVisibility(View.VISIBLE);
        tvFabNote.setVisibility(View.VISIBLE);
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;
        fabAdd.setVisibility(View.VISIBLE);
        fabClose.setVisibility(View.GONE);

        fabSchedule.setVisibility(View.GONE);
        tvFabSchedule.setVisibility(View.GONE);
        fabNote.setVisibility(View.GONE);
        tvFabNote.setVisibility(View.GONE);
    }
}