package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.adapter.ScheduleAdapter;
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.model.ScheduleInvitation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleFragment extends Fragment implements ScheduleAdapter.OnScheduleItemClickListener {

    private CalendarView calendarView;
    private TextView tvNoSchedule1, tvNoSchedule2;
    private RecyclerView recyclerViewSchedule;
    private ScheduleAdapter scheduleAdapter;
    private LocalDataStore localDataStore;
    private String currentSelectedDate;
    private String currentUserId;
    private final List<Schedule> currentSchedulesList = new ArrayList<>();
    private final Map<String, ScheduleInvitation> invitationMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localDataStore = LocalDataStore.getInstance(requireContext());
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);

        if (currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        tvNoSchedule1 = view.findViewById(R.id.tv_no_schedules1);
        tvNoSchedule2 = view.findViewById(R.id.tv_no_schedules2);
        calendarView = view.findViewById(R.id.calendarView);
        recyclerViewSchedule = view.findViewById(R.id.recyclerView_schedule);

        setupRecyclerView();

        calendarView.setOnDateChangeListener((viewCalendar, year, month, dayOfMonth) -> {
            currentSelectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth);
            loadSchedulesForDate(currentSelectedDate);
        });

        loadInitialSchedules();
    }

    private void setupRecyclerView() {
        scheduleAdapter = new ScheduleAdapter(requireContext(), new ArrayList<>(), this);
        recyclerViewSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewSchedule.setAdapter(scheduleAdapter);
    }

    private void loadInitialSchedules() {
        LocalDate today = LocalDate.now();
        currentSelectedDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        loadSchedulesForDate(currentSelectedDate);
    }

    private void loadSchedulesForDate(String date) {
        if (currentUserId == null) return;

        currentSchedulesList.clear();
        invitationMap.clear();

        LocalDate selectedLocalDate = LocalDate.parse(date);
        long startMillis = selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMillis = selectedLocalDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<ScheduleInvitation> invitations = localDataStore.scheduleInvitations().getAcceptedIncomplete(currentUserId, 1, false);
        for (ScheduleInvitation invitation : invitations) {
            if (invitation.getScheduleId() == null) continue;

            Schedule schedule = localDataStore.schedules().findById(invitation.getScheduleId());
            if (schedule == null || schedule.getStartTime() == null) continue;

            long scheduleStart = schedule.getStartTime().getTime();
            long scheduleEnd = schedule.getEndTime() != null ? schedule.getEndTime().getTime() : scheduleStart;
            if (scheduleStart <= endMillis && scheduleEnd >= startMillis) {
                schedule.setMyInvitationId(invitation.getId());
                schedule.setMyCompletedStatus(invitation.getCompleted());
                invitationMap.put(schedule.getId(), invitation);
                currentSchedulesList.add(schedule);
            }
        }

        updateUI(currentSchedulesList);
    }

    private void updateUI(List<Schedule> list) {
        if (getActivity() == null) return;

        Collections.sort(list, Comparator.comparing(schedule -> schedule.getStartTime() == null ? Long.MAX_VALUE : schedule.getStartTime().getTime()));

        getActivity().runOnUiThread(() -> {
            if (scheduleAdapter != null) {
                scheduleAdapter.setData(list);
            }

            if (list.isEmpty()) {
                recyclerViewSchedule.setVisibility(View.GONE);
                tvNoSchedule1.setVisibility(View.VISIBLE);
                tvNoSchedule2.setVisibility(View.VISIBLE);
            } else {
                recyclerViewSchedule.setVisibility(View.VISIBLE);
                tvNoSchedule1.setVisibility(View.GONE);
                tvNoSchedule2.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onItemClick(Schedule schedule) {
        if (schedule == null || schedule.getId() == null) {
            Toast.makeText(getContext(), "Lỗi: Lịch trình không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        AddEditScheduleFragment addEditScheduleFragment = new AddEditScheduleFragment();
        Bundle bundle = new Bundle();
        bundle.putString("schedule_id", schedule.getId());
        addEditScheduleFragment.setArguments(bundle);

        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, addEditScheduleFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onCheckboxClick(Schedule schedule, boolean isChecked) {
        if (schedule == null || schedule.getMyInvitationId() == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID lời mời.", Toast.LENGTH_SHORT).show();
            return;
        }

        ScheduleInvitation invitation = localDataStore.scheduleInvitations().getByScheduleId(schedule.getId()).stream()
                .filter(item -> schedule.getMyInvitationId().equals(item.getId()))
                .findFirst().orElse(null);
        if (invitation != null) {
            invitation.setCompleted(isChecked);
            localDataStore.scheduleInvitations().insert(invitation);
        }
    }
}