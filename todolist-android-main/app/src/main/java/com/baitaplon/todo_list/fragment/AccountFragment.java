package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.activity.AuthActivity;
import com.baitaplon.todo_list.adapter.ScheduleAdapter;
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.model.ScheduleInvitation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";
    private TextView tvUserName, btnLogout, btnDeleteAccount, tvNoHistory;
    private ImageView btnBackAccount;
    private RecyclerView rvCompletedSchedules;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ScheduleAdapter historyAdapter;
    private List<Schedule> historyList;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Ánh xạ view
        tvUserName = view.findViewById(R.id.tv_Tentk);
        btnLogout = view.findViewById(R.id.btn_Dangxuat);
        btnDeleteAccount = view.findViewById(R.id.btn_XoaTK);
        btnBackAccount = view.findViewById(R.id.btn_Trolai);
        rvCompletedSchedules = view.findViewById(R.id.rv_completed_schedules);
        tvNoHistory = view.findViewById(R.id.tv_no_history);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            currentUserId = user.getUid();
            String name = user.getDisplayName();
            tvUserName.setText((name != null && !name.isEmpty()) ? name : user.getEmail());

            // Load lịch sử
            setupRecyclerView();
            loadCompletedHistory();
        } else {
            tvUserName.setText("User");
        }

        // 🔹 Nút quay lại
        btnBackAccount.setOnClickListener(v -> requireActivity().onBackPressed());

        // 🔹 Đăng xuất
        btnLogout.setOnClickListener(v -> handleLogout());

        // 🔹 Xóa tài khoản
        btnDeleteAccount.setOnClickListener(v -> handleDeleteAccount(user));

        return view;
    }

    private void setupRecyclerView() {
        historyList = new ArrayList<>();
        // Adapter cho lịch sử chỉ cần xem, không cần click action phức tạp
        historyAdapter = new ScheduleAdapter(getContext(), historyList, new ScheduleAdapter.OnScheduleItemClickListener() {
            @Override
            public void onItemClick(Schedule schedule) {
                // Có thể mở chi tiết nếu muốn
            }

            @Override
            public void onCheckboxClick(Schedule schedule, boolean isChecked) {
                // Trong lịch sử đã hoàn thành, có thể chặn bỏ tick hoặc xử lý logic undo complete tại đây
                // Hiện tại để trống hoặc hiện thông báo
                Toast.makeText(getContext(), "Mục này đã được lưu vào lịch sử.", Toast.LENGTH_SHORT).show();
                historyAdapter.notifyDataSetChanged(); // Reset checkbox state visually
            }
        });

        rvCompletedSchedules.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCompletedSchedules.setAdapter(historyAdapter);
    }

    private void loadCompletedHistory() {
        if (currentUserId == null) return;

        // Tính thời gian 30 ngày trước
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Date date30DaysAgo = calendar.getTime();

        historyList.clear();

        // Task 1: Lấy lịch tôi tạo (Host) và đã hoàn thành
        Task<List<Schedule>> taskHost = db.collection("schedules")
                .whereEqualTo("hostId", currentUserId)
                .whereEqualTo("completed", true)
                .get()
                .continueWith(task -> {
                    List<Schedule> list = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Schedule s = doc.toObject(Schedule.class);
                            s.setMyCompletedStatus(true); // Đánh dấu để hiện checkbox
                            // Lọc client-side ngày tháng để tránh lỗi Index Firestore
                            if (s.getStartTime() != null && s.getStartTime().toDate().after(date30DaysAgo)) {
                                list.add(s);
                            }
                        }
                    }
                    return list;
                });

        // Task 2: Lấy lịch tôi được mời và đã hoàn thành (query bảng invitations)
        Task<List<Schedule>> taskInvited = db.collection("schedule_invitations")
                .whereEqualTo("invitedUserId", currentUserId)
                .whereEqualTo("completed", true) // Đã hoàn thành cá nhân
                .whereEqualTo("status", 1) // Đã chấp nhận
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult().isEmpty()) {
                        return Tasks.forResult(new ArrayList<Schedule>());
                    }

                    List<Task<DocumentSnapshot>> scheduleTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String scheduleId = doc.getString("scheduleId");
                        if (scheduleId != null) {
                            scheduleTasks.add(db.collection("schedules").document(scheduleId).get());
                        }
                    }

                    // Chờ lấy xong chi tiết các lịch
                    return Tasks.whenAllSuccess(scheduleTasks).continueWith(t -> {
                        List<Schedule> list = new ArrayList<>();
                        for (Object obj : t.getResult()) {
                            DocumentSnapshot doc = (DocumentSnapshot) obj;
                            if (doc.exists()) {
                                Schedule s = doc.toObject(Schedule.class);
                                if (s != null) {
                                    s.setMyCompletedStatus(true);
                                    // Lọc 30 ngày
                                    if (s.getStartTime() != null && s.getStartTime().toDate().after(date30DaysAgo)) {
                                        list.add(s);
                                    }
                                }
                            }
                        }
                        return list;
                    });
                });

        // Gộp cả 2 kết quả khi xong
        Tasks.whenAllSuccess(taskHost, taskInvited).addOnSuccessListener(results -> {
            List<Schedule> hostList = (List<Schedule>) results.get(0);
            List<Schedule> invitedList = (List<Schedule>) results.get(1);

            historyList.addAll(hostList);
            historyList.addAll(invitedList);

            // Sắp xếp: Mới nhất lên đầu
            Collections.sort(historyList, (s1, s2) -> {
                if (s1.getStartTime() == null || s2.getStartTime() == null) return 0;
                return s2.getStartTime().toDate().compareTo(s1.getStartTime().toDate());
            });

            historyAdapter.setData(historyList);

            // Hiển thị thông báo nếu rỗng
            if (historyList.isEmpty()) {
                tvNoHistory.setVisibility(View.VISIBLE);
                rvCompletedSchedules.setVisibility(View.GONE);
            } else {
                tvNoHistory.setVisibility(View.GONE);
                rvCompletedSchedules.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading history", e);
            Toast.makeText(getContext(), "Lỗi tải lịch sử!", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleLogout() {
        mAuth.signOut();
        if (getContext() != null) {
            getContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    .edit()
                    .remove("current_user_id")
                    .remove("current_user_name")
                    .clear()
                    .apply();
        }

        Intent intent = new Intent(getActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void handleDeleteAccount(FirebaseUser user) {
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Account deleted!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), AuthActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                } else {
                    Toast.makeText(getContext(), "Failed to delete account!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
        }
    }
}