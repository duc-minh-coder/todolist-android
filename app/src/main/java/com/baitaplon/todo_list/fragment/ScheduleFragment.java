package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.model.ScheduleInvitation;
import com.google.android.gms.tasks.Task; // Thêm Task
import com.google.android.gms.tasks.Tasks; // Thêm Tasks
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections; // Thêm Collections
import java.util.Comparator; // Thêm Comparator
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet; // Dùng HashSet để tránh trùng lặp
import java.util.List;
import java.util.Map;
import java.util.Set;      // Dùng Set

public class ScheduleFragment extends Fragment implements ScheduleAdapter.OnScheduleItemClickListener {

    private static final String TAG = "ScheduleFragment";

    private CalendarView calendarView;
    private TextView tvNoSchedule1, tvNoSchedule2;
    private RecyclerView recyclerViewSchedule;
    private ScheduleAdapter scheduleAdapter;
    private FirebaseFirestore db;
    private ListenerRegistration invitationListener; // Lời mời  đã chấp nhận (status=1, completed=false)
    private ListenerRegistration scheduleDetailsListener; // Chi tiết các lịch
    private String currentSelectedDate;
    private String currentUserId;
    private List<Schedule> currentSchedulesList = new ArrayList<>();
    // Map để nối invitation với schedule
    private Map<String, ScheduleInvitation> invitationMap = new HashMap<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
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
            String selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth);
            currentSelectedDate = selectedDate;
            loadSchedulesForDate(currentSelectedDate);
        });

        loadInitialSchedules();

        getParentFragmentManager().setFragmentResultListener("schedule_edit_result", this, (requestKey, bundle) -> {
            boolean isSaved = bundle.getBoolean("schedule_saved");
            if (isSaved && currentSelectedDate != null) {
                Log.d(TAG, "Received schedule saved result, Firestore listener will update UI.");
            }
        });
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
        Log.d(TAG, "Loading schedules for date: " + date);
        if (currentUserId == null) return;

        // Hủy các listener cũ
        if (invitationListener != null) invitationListener.remove();
        if (scheduleDetailsListener != null) scheduleDetailsListener.remove();

        // Xóa danh sách cũ
        currentSchedulesList.clear();
        invitationMap.clear();
        updateUI(currentSchedulesList);

        // Tạo mốc thời gian
        LocalDate selectedLocalDate = LocalDate.parse(date);
        LocalDateTime startOfDay = selectedLocalDate.atStartOfDay();
        LocalDateTime endOfDay = selectedLocalDate.atTime(23, 59, 59, 999999999);
        Timestamp startTimestamp = TimestampUtil.localDateTimeToTimestamp(startOfDay);
        Timestamp endTimestamp = TimestampUtil.localDateTimeToTimestamp(endOfDay);


        // --- Query 1: Lấy ID các lịch trình tôi tham gia (đã chấp nhận, chưa hoàn thành) ---
        // Logic: invitedUserId == me AND status == 1 AND completed == false
        Query invitationsQuery = db.collection("schedule_invitations")
                .whereEqualTo("invitedUserId", currentUserId)
                .whereEqualTo("status", 1) // 1 = Đã chấp nhận
                .whereEqualTo("completed", false); // Chỉ lấy lịch chưa hoàn thành

        invitationListener = invitationsQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for invitations.", e);
                if (e.getMessage() != null && e.getMessage().contains("index")) {
                    Log.e(TAG, " Firestore index required for invitations query: " + e.getMessage());
                    Toast.makeText(getContext(), "Cần tạo index cho lời mời, kiểm tra Logcat!", Toast.LENGTH_LONG).show();
                }
                return;
            }

            List<String> acceptedScheduleIds = new ArrayList<>();
            invitationMap.clear(); // Xóa map cũ

            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    ScheduleInvitation inv = doc.toObject(ScheduleInvitation.class);
                    inv.setId(doc.getId()); // Gán ID của document

                    if (inv.getScheduleId() != null) {
                        acceptedScheduleIds.add(inv.getScheduleId());
                        invitationMap.put(inv.getScheduleId(), inv); // Lưu lời mời vào map
                    }
                }
            }

            loadScheduleDetails(acceptedScheduleIds, startTimestamp, endTimestamp);
        });
    }

    //Query 2: Lấy chi tiết các lịch trình
    private void loadScheduleDetails(List<String> scheduleIds, Timestamp startTimestamp, Timestamp endTimestamp) {
        if (scheduleDetailsListener != null) {
            scheduleDetailsListener.remove();
        }
        currentSchedulesList.clear();

        if (scheduleIds == null || scheduleIds.isEmpty()) {
            updateUI(currentSchedulesList);
            return;
        }

        Query detailsQuery = db.collection("schedules")
                .whereIn(FieldPath.documentId(), scheduleIds);


        scheduleDetailsListener = detailsQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for schedule details.", e);
                if (e.getMessage() != null && e.getMessage().contains("index")) {
                    Log.e(TAG, " Firestore index required for details query: " + e.getMessage());
                    Toast.makeText(getContext(), "Cần tạo index cho chi tiết lịch, kiểm tra Logcat!", Toast.LENGTH_LONG).show();
                }
                return;
            }

            currentSchedulesList.clear();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Schedule schedule = doc.toObject(Schedule.class);

                    if (schedule.getStartTime() == null) {
                        continue;
                    }

                    boolean startsBeforeEndOfDay = schedule.getStartTime().compareTo(endTimestamp) <= 0;

                    boolean endsAfterStartOfDay = schedule.getEndTime() != null &&
                            schedule.getEndTime().compareTo(startTimestamp) >= 0;

                    boolean noEndTimeAndInDay = schedule.getEndTime() == null &&
                            schedule.getStartTime().compareTo(startTimestamp) >= 0 &&
                            schedule.getStartTime().compareTo(endTimestamp) <= 0;

                    if (startsBeforeEndOfDay && (endsAfterStartOfDay || noEndTimeAndInDay)) {

                        ScheduleInvitation myInv = invitationMap.get(schedule.getId());
                        if (myInv != null) {
                            schedule.setMyInvitationId(myInv.getId());
                            schedule.setMyCompletedStatus(myInv.getCompleted());
                        }
                        currentSchedulesList.add(schedule);
                    }
                }
            }
            Log.d(TAG, "Accepted schedule details updated (filtered): " + currentSchedulesList.size());
            updateUI(currentSchedulesList);
        });
    }

    private void updateUI(List<Schedule> list) {
        if (getActivity() == null) return;
        Collections.sort(list, Comparator.comparing(schedule -> {
            if (schedule == null || schedule.getStartTime() == null) {
                return new Timestamp(Long.MAX_VALUE, 999999999);
            }
            return schedule.getStartTime();
        }));

        getActivity().runOnUiThread(() -> {
            if (scheduleAdapter != null) {
                scheduleAdapter.setData(list);
                Log.d(TAG, "UI updated with list size: " + list.size());
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
        String myInvitationId = schedule.getMyInvitationId();
        // Cập nhật trường "completed" (hoặc "isCompleted", tùy theo tên trường của bạn)
        db.collection("schedule_invitations").document(myInvitationId)
                .update("completed", isChecked)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Checkbox updated successfully for invitation: " + myInvitationId))
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (invitationListener != null) {
            invitationListener.remove();
        }
        if (scheduleDetailsListener != null) {
            scheduleDetailsListener.remove();
        }
    }

    public static class TimestampUtil {
        public static Timestamp localDateTimeToTimestamp(LocalDateTime localDateTime) {
            if (localDateTime == null) return null;
            Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            return new Timestamp(date);
        }

        public static LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
            if (timestamp == null) return null;
            Date date = timestamp.toDate();
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
    }
}