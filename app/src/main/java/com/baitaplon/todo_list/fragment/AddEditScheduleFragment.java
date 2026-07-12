package com.baitaplon.todo_list.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.adapter.InvitedUserAdapter;
import com.baitaplon.todo_list.adapter.UserSearchAdapter;
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.model.Notification;
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.model.ScheduleInvitation;
import com.baitaplon.todo_list.model.User;
import com.baitaplon.todo_list.util.AlarmScheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AddEditScheduleFragment extends Fragment implements UserSearchAdapter.OnInviteClickListener, InvitedUserAdapter.OnRemoveUserClickListener {

    private ImageView btnBack, btnSave, btnShowInviteDialog;
    private CheckBox cbCompleteTop;
    private EditText edtTitle, edtPlace, edtNotes;
    private SwitchCompat switchAllDay, switchRepeat;
    private RelativeLayout layoutStartTime, layoutEndTime;
    private TextView tvStartTime, tvEndTime;
    private Spinner spinnerAlarm;
    private RecyclerView recyclerViewInvitedUsers;

    private LocalDataStore localDataStore;
    private String currentUserId;
    private String currentUsername;
    private String scheduleIdToEdit;
    private Calendar startCalendar;
    private Calendar endCalendar;
    private UserSearchAdapter searchAdapter;
    private InvitedUserAdapter invitedUserAdapter;
    private final List<User> invitedUsersList = new ArrayList<>();
    private final List<User> searchResults = new ArrayList<>();
    private final Set<String> invitedUserIds = new HashSet<>();
    private AlertDialog inviteDialog;
    private boolean isOwner = true;
    private boolean isNewSchedule = true;
    private String myInvitationId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_addedit_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localDataStore = LocalDataStore.getInstance(requireContext());
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);
        currentUsername = sharedPreferences.getString("current_user_name", "Unknown User");

        if (currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Người dùng chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            closeFragment();
            return;
        }

        initViews(view);
        setupSpinner();
        setupInvitedUserRecycler();

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR, 1);
        invitedUserIds.add(currentUserId);

        if (getArguments() != null) {
            scheduleIdToEdit = getArguments().getString("schedule_id");
            isNewSchedule = scheduleIdToEdit == null;
        }

        if (!isNewSchedule) {
            loadScheduleData(scheduleIdToEdit);
        } else {
            updateDateTimeViews();
        }

        setupClickListeners();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back_schedule);
        btnSave = view.findViewById(R.id.btn_save_schedule);
        cbCompleteTop = view.findViewById(R.id.checkbox_complete_top);
        edtTitle = view.findViewById(R.id.edt_schedule_title);
        edtPlace = view.findViewById(R.id.edt_schedule_place);
        edtNotes = view.findViewById(R.id.edt_schedule_notes);
        switchAllDay = view.findViewById(R.id.switch_all_day);
        layoutStartTime = view.findViewById(R.id.layout_start_time);
        layoutEndTime = view.findViewById(R.id.layout_end_time);
        tvStartTime = view.findViewById(R.id.tv_start_time);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        switchRepeat = view.findViewById(R.id.switch_repeat);
        spinnerAlarm = view.findViewById(R.id.spinner_alarm);
        btnShowInviteDialog = view.findViewById(R.id.btn_show_invite_dialog);
        recyclerViewInvitedUsers = view.findViewById(R.id.recycler_view_invited_users);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.alarm_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlarm.setAdapter(adapter);
    }

    private void setupInvitedUserRecycler() {
        invitedUserAdapter = new InvitedUserAdapter(invitedUsersList, this);
        recyclerViewInvitedUsers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewInvitedUsers.setAdapter(invitedUserAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> closeFragment());
        btnSave.setOnClickListener(v -> saveSchedule());
        switchAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> updateDateTimeViews());
        layoutStartTime.setOnClickListener(v -> showDatePicker(startCalendar));
        layoutEndTime.setOnClickListener(v -> showDatePicker(endCalendar));
        btnShowInviteDialog.setOnClickListener(v -> showInviteUserDialog());
    }

    private void updateDateTimeViews() {
        String pattern = switchAllDay.isChecked() ? "dd/MM/yyyy" : "dd/MM/yyyy HH:mm";
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault());
        tvStartTime.setText(formatter.format(startCalendar.getTime()));
        tvEndTime.setText(formatter.format(endCalendar.getTime()));
    }

    private void showDatePicker(final Calendar targetCalendar) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            targetCalendar.set(Calendar.YEAR, year);
            targetCalendar.set(Calendar.MONTH, month);
            targetCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            if (switchAllDay.isChecked()) {
                updateDateTimeViews();
            } else {
                showTimePicker(targetCalendar);
            }
        }, targetCalendar.get(Calendar.YEAR), targetCalendar.get(Calendar.MONTH), targetCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker(final Calendar targetCalendar) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            targetCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            targetCalendar.set(Calendar.MINUTE, minute);
            targetCalendar.set(Calendar.SECOND, 0);
            updateDateTimeViews();
        }, targetCalendar.get(Calendar.HOUR_OF_DAY), targetCalendar.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    private void loadScheduleData(String scheduleId) {
        Schedule schedule = localDataStore.schedules().findById(scheduleId);
        if (schedule == null) {
            Toast.makeText(getContext(), "Không tìm thấy lịch trình.", Toast.LENGTH_SHORT).show();
            closeFragment();
            return;
        }

        isOwner = schedule.getHostId() != null && schedule.getHostId().equals(currentUserId);
        invitedUserIds.clear();
        if (schedule.getInvitedUserIds() != null) {
            invitedUserIds.addAll(schedule.getInvitedUserIds());
        }
        invitedUserIds.add(currentUserId);

        edtTitle.setText(schedule.getTitle());
        edtPlace.setText(schedule.getPlace());
        edtNotes.setText(schedule.getNotes());
        if (schedule.getStartTime() != null) startCalendar.setTime(schedule.getStartTime());
        if (schedule.getEndTime() != null) endCalendar.setTime(schedule.getEndTime());
        switchRepeat.setChecked(Boolean.TRUE.equals(schedule.getRepeat()));
        spinnerAlarm.setSelection(schedule.getAlarmOption() != null ? schedule.getAlarmOption() : 0);

        ScheduleInvitation myInvitation = localDataStore.scheduleInvitations().findByScheduleAndUser(schedule.getId(), currentUserId);
        if (myInvitation != null) {
            myInvitationId = myInvitation.getId();
            cbCompleteTop.setChecked(Boolean.TRUE.equals(myInvitation.getCompleted()));
        }

        fetchInvitedUsersInfo(new ArrayList<>(invitedUserIds));
        if (!isOwner) {
            disableEditing();
        }
        updateDateTimeViews();
    }

    private void disableEditing() {
        edtTitle.setEnabled(false);
        edtPlace.setEnabled(false);
        edtNotes.setEnabled(false);
        switchAllDay.setEnabled(false);
        switchRepeat.setEnabled(false);
        spinnerAlarm.setEnabled(false);
        layoutStartTime.setClickable(false);
        layoutEndTime.setClickable(false);
        btnShowInviteDialog.setVisibility(View.GONE);
        invitedUserAdapter.setReadOnly(true);
    }

    private void fetchInvitedUsersInfo(List<String> userIds) {
        invitedUsersList.clear();
        for (String userId : userIds) {
            User user = localDataStore.users().findById(userId);
            if (user != null) {
                invitedUsersList.add(user);
            }
        }
        invitedUserAdapter.setData(invitedUsersList);
    }

    private void showInviteUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = requireActivity().getLayoutInflater().inflate(R.layout.dialog_invite_user, null);

        EditText edtSearchUser = dialogView.findViewById(R.id.edt_search_user);
        RecyclerView rvSearchResults = dialogView.findViewById(R.id.recycler_view_search_results);
        TextView tvNoResults = dialogView.findViewById(R.id.tv_no_results);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar_search);

        searchResults.clear();
        searchAdapter = new UserSearchAdapter(searchResults, this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(searchAdapter);

        edtSearchUser.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() > 2) {
                    searchUsers(query, rvSearchResults, tvNoResults, progressBar);
                } else {
                    searchResults.clear();
                    searchAdapter.setData(searchResults);
                    progressBar.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        builder.setView(dialogView);
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());
        inviteDialog = builder.create();
        inviteDialog.show();
    }

    private void searchUsers(String query, RecyclerView rvSearchResults, TextView tvNoResults, ProgressBar progressBar) {
        searchResults.clear();
        for (User user : localDataStore.users().search(query)) {
            if (!user.getUid().equals(currentUserId)) {
                searchResults.add(user);
            }
        }
        searchAdapter.setData(searchResults);
        progressBar.setVisibility(View.GONE);
        if (searchResults.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
        } else {
            tvNoResults.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onInviteClick(User user) {
        if (user.getUid() == null || invitedUserIds.contains(user.getUid())) {
            Toast.makeText(getContext(), user.getUsername() + " đã được mời rồi", Toast.LENGTH_SHORT).show();
            return;
        }
        invitedUserIds.add(user.getUid());
        invitedUsersList.add(user);
        invitedUserAdapter.notifyItemInserted(invitedUsersList.size() - 1);
    }

    @Override
    public void onRemoveUser(User user, int position) {
        if (user == null || user.getUid() == null) return;
        if (user.getUid().equals(currentUserId)) {
            Toast.makeText(getContext(), "Bạn không thể xóa chính mình.", Toast.LENGTH_SHORT).show();
            return;
        }
        invitedUsersList.remove(position);
        invitedUserIds.remove(user.getUid());
        invitedUserAdapter.notifyItemRemoved(position);
        invitedUserAdapter.notifyItemRangeChanged(position, invitedUsersList.size());
    }

    private void saveSchedule() {
        if (!isOwner) {
            Toast.makeText(getContext(), "Bạn không có quyền chỉnh sửa lịch trình này.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startCalendar.getTime().after(endCalendar.getTime())) {
            Toast.makeText(getContext(), "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        Schedule schedule = new Schedule();
        schedule.setId(scheduleIdToEdit != null ? scheduleIdToEdit : UUID.randomUUID().toString());
        schedule.setHostId(currentUserId);
        schedule.setHostUsername(currentUsername);
        schedule.setTitle(title);
        schedule.setStartTime(startCalendar.getTime());
        schedule.setEndTime(endCalendar.getTime());
        schedule.setPlace(edtPlace.getText().toString().trim());
        schedule.setNotes(edtNotes.getText().toString().trim());
        schedule.setRepeat(switchRepeat.isChecked());
        schedule.setAlarmOption(spinnerAlarm.getSelectedItemPosition());
        schedule.setInvitedUserIds(new ArrayList<>(invitedUserIds));
        schedule.setMyCompletedStatus(cbCompleteTop.isChecked());
        schedule.setMyInvitationId(myInvitationId);
        localDataStore.schedules().insert(schedule);

        ensureHostInvitation(schedule, title);
        createInvitations(schedule, title);
        AlarmScheduler.scheduleAlarm(requireContext(), schedule);
        closeFragment();
    }

    private void ensureHostInvitation(Schedule schedule, String title) {
        ScheduleInvitation hostInvitation = localDataStore.scheduleInvitations().findByScheduleAndUser(schedule.getId(), currentUserId);
        if (hostInvitation == null) {
            hostInvitation = new ScheduleInvitation();
            hostInvitation.setId(UUID.randomUUID().toString());
            hostInvitation.setScheduleId(schedule.getId());
            hostInvitation.setHostId(currentUserId);
            hostInvitation.setInvitedUserId(currentUserId);
        }
        hostInvitation.setCompleted(cbCompleteTop.isChecked());
        hostInvitation.setStatus(1);
        hostInvitation.setScheduleTitle(title);
        localDataStore.scheduleInvitations().insert(hostInvitation);
    }

    private void createInvitations(Schedule schedule, String title) {
        for (String invitedUid : invitedUserIds) {
            if (invitedUid.equals(currentUserId)) continue;
            ScheduleInvitation invitation = localDataStore.scheduleInvitations().findByScheduleAndUser(schedule.getId(), invitedUid);
            if (invitation == null) {
                invitation = new ScheduleInvitation();
                invitation.setId(UUID.randomUUID().toString());
                invitation.setScheduleId(schedule.getId());
                invitation.setHostId(currentUserId);
                invitation.setInvitedUserId(invitedUid);
                invitation.setStatus(0);
                invitation.setCompleted(false);
                invitation.setScheduleTitle(title);
                localDataStore.scheduleInvitations().insert(invitation);

                Notification notification = new Notification();
                notification.setId(UUID.randomUUID().toString());
                notification.setUserId(invitedUid);
                notification.setType("schedule_invitation");
                notification.setMessage(currentUsername + " đã mời bạn tham gia: " + title);
                notification.setReferenceId(invitation.getId());
                notification.setRead(false);
                notification.setCreatedAt(new Date());
                localDataStore.notifications().insert(notification);
            }
        }
    }

    private void closeFragment() {
        FragmentManager fm = getParentFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }
    }
}