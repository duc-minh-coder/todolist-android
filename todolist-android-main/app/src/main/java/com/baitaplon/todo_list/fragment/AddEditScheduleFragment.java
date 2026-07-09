package com.baitaplon.todo_list.fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log; // Thêm Log
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

import com.baitaplon.todo_list.adapter.InvitedUserAdapter;
import com.baitaplon.todo_list.adapter.UserSearchAdapter;
import com.baitaplon.todo_list.model.User;
import com.baitaplon.todo_list.model.ScheduleInvitation;
import com.baitaplon.todo_list.model.Notification;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.model.Schedule;

import com.baitaplon.todo_list.util.AlarmScheduler;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AddEditScheduleFragment extends Fragment implements UserSearchAdapter.OnInviteClickListener, InvitedUserAdapter.OnRemoveUserClickListener {

    private static final String TAG = "AddEditScheduleFrag"; // Tag để Log

    ImageView btnBack, btnSave;
    CheckBox cbCompleteTop;
    EditText edtTitle, edtPlace, edtNotes;
    SwitchCompat switchAllDay;
    RelativeLayout layoutStartTime, layoutEndTime;
    TextView tvStartTime, tvEndTime;
    SwitchCompat switchRepeat;
    Spinner spinnerAlarm;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String currentUsername;
    private String scheduleIdToEdit = null; // Lưu ID nếu là chế độ Edit
    private Calendar startCalendar, endCalendar;
    private ImageView btnShowInviteDialog;
    private RecyclerView recyclerViewInvitedUsers;
    private List<User> invitedUsersList = new ArrayList<>();
    private Set<String> invitedUserIds = new HashSet<>(); // Chỉ lưu ID để set vào model
    private UserSearchAdapter searchAdapter;
    private InvitedUserAdapter invitedUserAdapter;
    private List<User> searchResults = new ArrayList<>();
    private AlertDialog inviteDialog; // Biến lưu dialog
    private boolean isOwner = false;
    private String myInvitationId = null;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm", new Locale("vi", "VN"));
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy", new Locale("vi", "VN"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_addedit_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null); // Lấy String UID

        if (currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Người dùng chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            closeFragment();
            return;
        }

        invitedUserIds.add(currentUserId);

        // TODO: Lấy currentUsername (ví dụ từ Firestore hoặc lưu trong SharedPreferences khi login)
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            fetchUsername(currentUserId);
        }

        initViews(view);
        setupSpinner();
        setupInvitedUserRecycler();

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();
        endCalendar.add(Calendar.HOUR, 1);

        if (getArguments() != null && getArguments().containsKey("schedule_id")) {
            scheduleIdToEdit = getArguments().getString("schedule_id", null);
            if (scheduleIdToEdit != null) {
                loadScheduleData(scheduleIdToEdit);
            } else {
                isOwner = true;
                updateDateTimeViews();
            }
        } else {
            isOwner = true;
            updateDateTimeViews();
        }

        setupClickListeners();
    }

    private void fetchUsername(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                        if (currentUsername == null || currentUsername.isEmpty()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            currentUsername = (user != null) ? user.getEmail() : "Unknown User";
                        }
                    } else {
                        Log.w(TAG, "User document not found for username fetch");
                        FirebaseUser user = mAuth.getCurrentUser();
                        currentUsername = (user != null) ? user.getEmail() : "Unknown User";
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching username", e);
                    FirebaseUser user = mAuth.getCurrentUser();
                    currentUsername = (user != null) ? user.getEmail() : "Unknown User";
                });
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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.alarm_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlarm.setAdapter(adapter);
    }

    private void setupInvitedUserRecycler() {
        invitedUserAdapter = new InvitedUserAdapter(invitedUsersList, this);
        recyclerViewInvitedUsers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewInvitedUsers.setAdapter(invitedUserAdapter);
    }

    @Override
    public void onRemoveUser(User user, int position) {
        if (user == null || user.getUid() == null) return;
        if (user.getUid().equals(currentUserId)) {
            Toast.makeText(getContext(), "Bạn không thể xóa chính mình.", Toast.LENGTH_SHORT).show();
            return;
        }
        invitedUsersList.remove(position);
        invitedUserIds.remove(user.getUid()); // Xóa ID khỏi Set

        invitedUserAdapter.notifyItemRemoved(position);
        invitedUserAdapter.notifyItemRangeChanged(position, invitedUsersList.size()); // Cập nhật lại vị trí
    }

    private void updateDateTimeViews() {
        if (switchAllDay.isChecked()) {
            tvStartTime.setText(dateFormat.format(startCalendar.getTime()));
            tvEndTime.setText(dateFormat.format(endCalendar.getTime()));
        } else {
            tvStartTime.setText(dateTimeFormat.format(startCalendar.getTime()));
            tvEndTime.setText(dateTimeFormat.format(endCalendar.getTime()));
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> closeFragment());
        btnSave.setOnClickListener(v -> saveSchedule());

        switchAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDateTimeViews();
        });

        layoutStartTime.setOnClickListener(v -> showDateTimePicker(startCalendar, tvStartTime));
        layoutEndTime.setOnClickListener(v -> showDateTimePicker(endCalendar, tvEndTime));
        btnShowInviteDialog.setOnClickListener(v -> showInviteUserDialog());
    }

    private void showInviteUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_invite_user, null);

        EditText edtSearchUser = dialogView.findViewById(R.id.edt_search_user);
        RecyclerView rvSearchResults = dialogView.findViewById(R.id.recycler_view_search_results);
        TextView tvNoResults = dialogView.findViewById(R.id.tv_no_results);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar_search);

        // Setup RecyclerView cho kết quả tìm kiếm
        searchResults.clear();
        searchAdapter = new UserSearchAdapter(searchResults, this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(searchAdapter);

        // Sự kiện TextChanged
        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() > 2) { // Chỉ tìm khi gõ > 2 ký tự
                    progressBar.setVisibility(View.VISIBLE);
                    tvNoResults.setVisibility(View.GONE);
                    rvSearchResults.setVisibility(View.GONE);
                    searchUsersInFirestore(query, rvSearchResults, tvNoResults, progressBar);
                } else {
                    searchResults.clear();
                    searchAdapter.setData(searchResults);
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        builder.setView(dialogView);
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());

        inviteDialog = builder.create();
        inviteDialog.show();
    }

    private void searchUsersInFirestore(String query, RecyclerView rv, TextView tvNoResults, ProgressBar pb) {
        // Tìm theo email
        Query queryByEmail = db.collection("users")
                .whereGreaterThanOrEqualTo("email", query)
                .whereLessThanOrEqualTo("email", query + "\uf8ff")
                .limit(10); // Giới hạn 10 kết quả

        // TODO: chạy thêm 1 query nữa tìm theo 'username'

        queryByEmail.get().addOnSuccessListener(queryDocumentSnapshots -> {
            searchResults.clear();
            if (queryDocumentSnapshots.isEmpty()) {
                // Không tìm thấy
                tvNoResults.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                // Tìm thấy
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    User user = doc.toObject(User.class);
                    // Không mời chính mình
                    if (!user.getEmail().equals(mAuth.getCurrentUser().getEmail())) {
                        searchResults.add(user);
                    }
                }
                searchAdapter.setData(searchResults);
                tvNoResults.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
            pb.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi khi tìm user: ", e);
            tvNoResults.setText("Lỗi tìm kiếm.");
            tvNoResults.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            pb.setVisibility(View.GONE);
        });
    }

    @Override
    public void onInviteClick(User user) {
        String uid = user.getUid();
        if (uid == null) {
            uid = user.getEmail();
        }

        if (invitedUserIds.add(uid)) {
            invitedUsersList.add(user);
            invitedUserAdapter.notifyItemInserted(invitedUsersList.size() - 1);
            Toast.makeText(getContext(), "Đã thêm " + user.getUsername(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), user.getUsername() + " đã được mời rồi", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDateTimePicker(final Calendar calendarToSet, final TextView textViewToUpdate) {
        int year = calendarToSet.get(Calendar.YEAR);
        int month = calendarToSet.get(Calendar.MONTH);
        int day = calendarToSet.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, yearSelected, monthOfYear, dayOfMonth) -> {
                    calendarToSet.set(Calendar.YEAR, yearSelected);
                    calendarToSet.set(Calendar.MONTH, monthOfYear);
                    calendarToSet.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    if (!switchAllDay.isChecked()) {
                        showTimePicker(calendarToSet, textViewToUpdate);
                    } else {
                        calendarToSet.set(Calendar.HOUR_OF_DAY, 0);
                        calendarToSet.set(Calendar.MINUTE, 0);
                        calendarToSet.set(Calendar.SECOND, 0);
                        updateDateTimeViews();
                    }
                }, year, month, day);

        datePickerDialog.show();
    }

    private void showTimePicker(final Calendar calendarToSet, final TextView textViewToUpdate) {
        int hour = calendarToSet.get(Calendar.HOUR_OF_DAY);
        int minute = calendarToSet.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    calendarToSet.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendarToSet.set(Calendar.MINUTE, minuteOfHour);
                    calendarToSet.set(Calendar.SECOND, 0);

                    updateDateTimeViews();
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void loadScheduleData(String scheduleId) {
        db.collection("schedules").document(scheduleId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.w(TAG, "Schedule document not found: " + scheduleId);
                        Toast.makeText(getContext(), "Không tìm thấy lịch trình.", Toast.LENGTH_SHORT).show();
                        closeFragment();
                        return;
                    }

                    Schedule schedule = documentSnapshot.toObject(Schedule.class);
                    if (schedule == null) {
                        Log.e(TAG, "Schedule object is null after conversion.");
                        return;
                    }

                    // Kiểm tra quyền sở hữu
                    isOwner = (schedule.getHostId() != null && schedule.getHostId().equals(currentUserId));

                    if (schedule.getInvitedUserIds() != null) {
                        invitedUserIds = new HashSet<>(schedule.getInvitedUserIds());
                    }
                    invitedUserIds.add(currentUserId);
                    loadMyInvitationStatus(scheduleId);
                    loadAcceptedUsers(scheduleId);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            edtTitle.setText(schedule.getTitle());
                            edtPlace.setText(schedule.getPlace());
                            edtNotes.setText(schedule.getNotes());

                            // Cập nhật Calendar
                            if (schedule.getStartTime() != null) {
                                startCalendar.setTime(schedule.getStartTime().toDate());
                            }
                            if (schedule.getEndTime() != null) {
                                endCalendar.setTime(schedule.getEndTime().toDate());
                            }
                            updateDateTimeViews();

                            switchRepeat.setChecked(schedule.getRepeat() != null && schedule.getRepeat());
                            int alarmOpt = (schedule.getAlarmOption() != null) ? schedule.getAlarmOption() : 0;
                            if (alarmOpt >= 0 && alarmOpt < spinnerAlarm.getCount()) {
                                spinnerAlarm.setSelection(alarmOpt);
                            }

                            if (!isOwner) {
                                disableEditing();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedule data", e);
                    Toast.makeText(getContext(), "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMyInvitationStatus(String scheduleId) {
        db.collection("schedule_invitations")
                .whereEqualTo("scheduleId", scheduleId)
                .whereEqualTo("invitedUserId", currentUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        myInvitationId = doc.getId(); // Lưu ID lời mời của tôi
                        Boolean completed = doc.getBoolean("completed");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                cbCompleteTop.setChecked(completed != null && completed);
                            });
                        }
                    } else {
                        Log.w(TAG, "Không tìm thấy lời mời của host cho schedule: " + scheduleId);
                    }
                });
    }

    private void disableEditing() {
        edtTitle.setEnabled(false);
        edtTitle.setFocusable(false);
        edtPlace.setEnabled(false);
        edtPlace.setFocusable(false);
        edtNotes.setEnabled(false);
        edtNotes.setFocusable(false);
        switchAllDay.setEnabled(false);
        switchRepeat.setEnabled(false);
        spinnerAlarm.setEnabled(false);
        layoutStartTime.setClickable(false);
        layoutEndTime.setClickable(false);
        btnShowInviteDialog.setVisibility(View.GONE);

        if (invitedUserAdapter != null) {
            invitedUserAdapter.setReadOnly(true);
        }
    }

    //Chỉ tải thông tin user ĐÃ CHẤP NHẬN (status == 1)
    private void loadAcceptedUsers(String scheduleId) {
        db.collection("schedule_invitations")
                .whereEqualTo("scheduleId", scheduleId)
                .whereEqualTo("status", 1) // 1 = Đã chấp nhận
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> acceptedUserIds = new ArrayList<>();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            // Lấy ID của user đã chấp nhận
                            String acceptedUid = doc.getString("invitedUserId");
                            if (acceptedUid != null) {
                                acceptedUserIds.add(acceptedUid);
                            }
                        }
                    }

                    fetchInvitedUsersInfo(acceptedUserIds);

                    // Nếu không có ai chấp nhận (acceptedUserIds rỗng),
                    if (acceptedUserIds.isEmpty()) {
                        Log.d(TAG, "No accepted users found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải danh sách người đã chấp nhận: ", e);
                });
    }

    private void fetchInvitedUsersInfo(List<String> userIds) {
        invitedUsersList.clear();

        if (userIds == null || userIds.isEmpty()) {
            if(getActivity() != null && invitedUserAdapter != null) {
                getActivity().runOnUiThread(() -> {
                    invitedUserAdapter.setData(invitedUsersList);
                });
            }
            return;
        }

        db.collection("users").whereIn(FieldPath.documentId(), userIds).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            User user = doc.toObject(User.class);
                            user.setUid(doc.getId());
                            invitedUsersList.add(user);
                        }
                    }
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (invitedUserAdapter != null) {
                                invitedUserAdapter.setData(invitedUsersList); // Cập nhật adapter với danh sách đã tìm thấy
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy thông tin user đã mời: ", e);
                });
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

        String place = edtPlace.getText().toString().trim();
        String notes = edtNotes.getText().toString().trim();
        boolean isAllDay = switchAllDay.isChecked();
        boolean myCompletedStatus = cbCompleteTop.isChecked();

        if (startCalendar.getTime().after(endCalendar.getTime())) {
            Toast.makeText(getContext(), "Ngày kết thúc phải sau ngày bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getEmail() : "Unknown User";
        }

        Timestamp startTimeStamp = new Timestamp(startCalendar.getTime());
        Timestamp endTimeStamp = new Timestamp(endCalendar.getTime());
        if (isAllDay) {
            Calendar startOfDay = (Calendar) startCalendar.clone();
            startOfDay.set(Calendar.HOUR_OF_DAY, 0);
            startOfDay.set(Calendar.MINUTE, 0);
            startOfDay.set(Calendar.SECOND, 0);
            startOfDay.set(Calendar.MILLISECOND, 0);
            startTimeStamp = new Timestamp(startOfDay.getTime());

            Calendar endOfDay = (Calendar) startOfDay.clone();
            endOfDay.add(Calendar.DAY_OF_YEAR, 1);
            endTimeStamp = new Timestamp(endOfDay.getTime());
        }

        boolean isRepeat = switchRepeat.isChecked();
        int alarmSelectionIndex = spinnerAlarm.getSelectedItemPosition();

        Schedule scheduleToSave = new Schedule();
        scheduleToSave.setHostId(currentUserId);
        scheduleToSave.setHostUsername(currentUsername);
        scheduleToSave.setTitle(title);
        scheduleToSave.setStartTime(startTimeStamp);
        scheduleToSave.setEndTime(endTimeStamp);
        scheduleToSave.setPlace(place);
        scheduleToSave.setNotes(notes);
        scheduleToSave.setRepeat(isRepeat);
        scheduleToSave.setAlarmOption(alarmSelectionIndex);
        scheduleToSave.setInvitedUserIds(new ArrayList<>(invitedUserIds));

        if (scheduleIdToEdit == null) {
            db.collection("schedules").add(scheduleToSave)
                    .addOnSuccessListener(documentReference -> {
                        String newScheduleId = documentReference.getId();
                        createInvitations(newScheduleId, title, myCompletedStatus);
                        scheduleToSave.setId(newScheduleId);
                        AlarmScheduler.scheduleAlarm(requireContext(), scheduleToSave);
                        notifySaveAndClose();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding schedule", e));
        } else {
            scheduleToSave.setId(scheduleIdToEdit);
            db.collection("schedules").document(scheduleIdToEdit).set(scheduleToSave)
                    .addOnSuccessListener(aVoid -> {
                        updateInvitations(scheduleIdToEdit, title, myCompletedStatus);
                        AlarmScheduler.scheduleAlarm(requireContext(), scheduleToSave);
                        notifySaveAndClose();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating schedule", e));
        }
    }

    private void createInvitations(String scheduleId, String scheduleTitle, boolean hostCompletedStatus) {
        WriteBatch batch = db.batch();

        for (String invitedUid : invitedUserIds) {
            ScheduleInvitation invitation = new ScheduleInvitation();
            invitation.setScheduleId(scheduleId);
            invitation.setHostId(currentUserId);
            invitation.setInvitedUserId(invitedUid);
            invitation.setScheduleTitle(scheduleTitle);

            if (invitedUid.equals(currentUserId)) {
                invitation.setStatus(1); // 1 = Accepted
                invitation.setCompleted(hostCompletedStatus); // Lấy từ checkbox
            } else {
                invitation.setStatus(0); // 0 = Pending
                invitation.setCompleted(false);
            }

            DocumentReference invRef = db.collection("schedule_invitations").document();
            batch.set(invRef, invitation);

            if (!invitedUid.equals(currentUserId)) {
                createNotification(batch, invitedUid, scheduleTitle, invRef.getId());
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Tạo " + invitedUserIds.size() + " lời mời thành công."))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi tạo batch lời mời", e));
    }

    private void updateInvitations(String scheduleId, String scheduleTitle, boolean hostCompletedStatus) {
        // Cập nhật lời mời của Host
        db.collection("schedule_invitations")
                .whereEqualTo("scheduleId", scheduleId)
                .whereEqualTo("invitedUserId", currentUserId)
                .limit(1).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String hostInvitationId = snapshots.getDocuments().get(0).getId();
                        db.collection("schedule_invitations").document(hostInvitationId)
                                .update("completed", hostCompletedStatus, "scheduleTitle", scheduleTitle);
                    } else {
                        ScheduleInvitation hostInv = new ScheduleInvitation();
                        hostInv.setScheduleId(scheduleId);
                        hostInv.setHostId(currentUserId);
                        hostInv.setInvitedUserId(currentUserId);
                        hostInv.setStatus(1); // 1 = Accepted
                        hostInv.setCompleted(hostCompletedStatus);
                        hostInv.setScheduleTitle(scheduleTitle);
                        db.collection("schedule_invitations").add(hostInv);
                    }
                });

        // Lấy TẤT CẢ lời mời hiện có của lịch trình này
        db.collection("schedule_invitations")
                .whereEqualTo("scheduleId", scheduleId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    Set<String> existingInvitedUserIds = new HashSet<>();
                    Map<String, String> existingInvitationDocIds = new HashMap<>(); // Map<UserId, DocumentId>

                    // Lưu lại danh sách những người đã có lời mời
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString("invitedUserId");
                        if (userId != null) {
                            existingInvitedUserIds.add(userId);
                            existingInvitationDocIds.put(userId, doc.getId());
                        }
                    }

                    WriteBatch batch = db.batch();
                    int newInvitationsCount = 0;

                    // Lặp qua danh sách MỚI (invitedUserIds từ UI)
                    for (String newUserId : invitedUserIds) {
                        if (newUserId.equals(currentUserId)) {
                            continue; // Bỏ qua host, vì đã xử lý ở bước 1
                        }

                        if (!existingInvitedUserIds.contains(newUserId)) {
                            ScheduleInvitation invitation = new ScheduleInvitation();
                            invitation.setScheduleId(scheduleId);
                            invitation.setHostId(currentUserId);
                            invitation.setInvitedUserId(newUserId);
                            invitation.setScheduleTitle(scheduleTitle);
                            invitation.setStatus(0); // 0 = Pending
                            invitation.setCompleted(false);

                            DocumentReference invRef = db.collection("schedule_invitations").document();
                            batch.set(invRef, invitation);

                            // Tạo thông báo
                            createNotification(batch, newUserId, scheduleTitle, invRef.getId());
                            newInvitationsCount++;

                        } else {
                            String docId = existingInvitationDocIds.get(newUserId);
                            if (docId != null) {
                                DocumentReference invRef = db.collection("schedule_invitations").document(docId);
                                batch.update(invRef, "scheduleTitle", scheduleTitle);
                            }
                        }
                    }

                    if (newInvitationsCount > 0) {
                        int finalNewInvitationsCount = newInvitationsCount;
                        batch.commit()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã tạo/cập nhật " + finalNewInvitationsCount + " lời mời mới."))
                                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi cập nhật batch lời mời", e));
                    } else if (!existingInvitationDocIds.isEmpty()) {
                        batch.commit().addOnFailureListener(e -> Log.e(TAG, "Lỗi khi cập nhật title lời mời cũ", e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy danh sách lời mời hiện có", e);
                });
    }

    private void createNotification(WriteBatch batch, String invitedUid, String scheduleTitle, String invitationId) {
        Notification notification = new Notification();
        notification.setUserId(invitedUid);
        notification.setType("schedule_invitation");
        notification.setMessage(currentUsername + " đã mời bạn tham gia: " + scheduleTitle);
        notification.setReferenceId(invitationId);
        notification.setRead(false);
        DocumentReference notifRef = db.collection("notifications").document();
        batch.set(notifRef, notification);
    }

    private void notifySaveAndClose() {
        Bundle result = new Bundle();
        result.putBoolean("schedule_saved", true);
        getParentFragmentManager().setFragmentResult("schedule_edit_result", result);

        if (getActivity() != null) {
            getActivity().runOnUiThread(this::closeFragment);
        }
    }

    private void closeFragment() {
        FragmentManager fm = getParentFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else if (getActivity() != null) {
            Log.w(TAG, "Attempting to finish Activity from AddEditScheduleFragment");
        }
    }
}