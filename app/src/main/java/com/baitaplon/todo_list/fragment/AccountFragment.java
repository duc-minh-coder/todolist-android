package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.activity.AuthActivity;
import com.baitaplon.todo_list.adapter.ScheduleAdapter;
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.model.Schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountFragment extends Fragment {

    private TextView tvUserName, btnLogout, btnDeleteAccount, tvNoHistory;
    private ImageView btnBackAccount;
    private RecyclerView rvCompletedSchedules;
    private ScheduleAdapter historyAdapter;
    private final List<Schedule> historyList = new ArrayList<>();
    private LocalDataStore localDataStore;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        localDataStore = LocalDataStore.getInstance(requireContext());
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);

        tvUserName = view.findViewById(R.id.tv_Tentk);
        btnLogout = view.findViewById(R.id.btn_Dangxuat);
        btnDeleteAccount = view.findViewById(R.id.btn_XoaTK);
        btnBackAccount = view.findViewById(R.id.btn_Trolai);
        rvCompletedSchedules = view.findViewById(R.id.rv_completed_schedules);
        tvNoHistory = view.findViewById(R.id.tv_no_history);

        String username = sharedPreferences.getString("current_user_name", "User");
        tvUserName.setText(username);

        setupRecyclerView();
        loadCompletedHistory();

        btnBackAccount.setOnClickListener(v -> requireActivity().onBackPressed());
        btnLogout.setOnClickListener(v -> handleLogout());
        btnDeleteAccount.setOnClickListener(v -> handleDeleteAccount());

        return view;
    }

    private void setupRecyclerView() {
        historyAdapter = new ScheduleAdapter(getContext(), historyList, new ScheduleAdapter.OnScheduleItemClickListener() {
            @Override
            public void onItemClick(Schedule schedule) {
            }

            @Override
            public void onCheckboxClick(Schedule schedule, boolean isChecked) {
                Toast.makeText(getContext(), "Lịch sử chỉ xem, không chỉnh sửa tại đây.", Toast.LENGTH_SHORT).show();
            }
        });
        rvCompletedSchedules.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCompletedSchedules.setAdapter(historyAdapter);
    }

    private void loadCompletedHistory() {
        historyList.clear();
        if (currentUserId == null) {
            historyAdapter.setData(historyList);
            return;
        }

        List<Schedule> ownedSchedules = localDataStore.schedules().getByHostId(currentUserId);
        for (Schedule schedule : ownedSchedules) {
            if (Boolean.TRUE.equals(schedule.isMyCompletedStatus())) {
                historyList.add(schedule);
            }
        }

        Collections.sort(historyList, (first, second) -> {
            if (first.getStartTime() == null || second.getStartTime() == null) return 0;
            return second.getStartTime().compareTo(first.getStartTime());
        });

        historyAdapter.setData(historyList);
        if (historyList.isEmpty()) {
            tvNoHistory.setVisibility(View.VISIBLE);
            rvCompletedSchedules.setVisibility(View.GONE);
        } else {
            tvNoHistory.setVisibility(View.GONE);
            rvCompletedSchedules.setVisibility(View.VISIBLE);
        }
    }

    private void handleLogout() {
        if (getContext() != null) {
            getContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit().clear().apply();
        }
        Intent intent = new Intent(getActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void handleDeleteAccount() {
        Toast.makeText(getContext(), "Xóa tài khoản local chưa được triển khai.", Toast.LENGTH_SHORT).show();
    }
}