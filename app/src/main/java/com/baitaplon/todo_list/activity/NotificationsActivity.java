package com.baitaplon.todo_list.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.adapter.NotificationAdapter;
import com.baitaplon.todo_list.model.Notification;
import com.baitaplon.todo_list.model.NoteInvitation;
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.model.ScheduleInvitation;
import com.baitaplon.todo_list.util.AlarmScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Date;
import java.util.UUID;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.NotificationClickListener {

    private static final String TAG = "NotificationsActivity";

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private TextView tvNoNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();

    private LocalDataStore localDataStore;
    private String currentUserId;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        localDataStore = LocalDataStore.getInstance(this);

        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);

        if (currentUserId == null) {
            Log.e(TAG, "User ID is null!");
            Toast.makeText(this, "Lỗi: Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish(); // Đóng Activity nếu chưa đăng nhập
            return;
        }
        fetchCurrentUsername();

        toolbar = findViewById(R.id.toolbar_notifications);
        recyclerView = findViewById(R.id.recycler_view_notifications);
        tvNoNotifications = findViewById(R.id.tv_no_notifications);

        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish()); // Nút back

        setupRecyclerView();

        loadNotifications();
    }

    private void fetchCurrentUsername() {
        currentUsername = localDataStore.users().findById(currentUserId) != null
                ? localDataStore.users().findById(currentUserId).getUsername()
                : "Một người dùng";
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, notificationList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        notificationList.clear();
        List<Notification> stored = localDataStore.notifications().getByUser(currentUserId);
        if (stored != null) {
            notificationList.addAll(stored);
        }
        adapter.setData(notificationList);
        checkIfEmpty();
    }

    // Kiểm tra và hiển thị "Không có thông báo"
    private void checkIfEmpty() {
        if (notificationList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvNoNotifications.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvNoNotifications.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAcceptClick(Notification notification, int position) {
        updateInvitationStatus(notification, 1);
        sendResponseNotification(notification, "đã chấp nhận");
        if ("schedule_invitation".equals(notification.getType())) {
            setAlarmForAcceptedSchedule(notification.getReferenceId());
        }
        deleteNotification(notification, position);
    }
    // Hàm xử lý lấy dữ liệu và đặt báo thức
    private void setAlarmForAcceptedSchedule(String invitationId) {
        if (invitationId == null) return;

        ScheduleInvitation invitation = localDataStore.scheduleInvitations().getByScheduleId(invitationId).stream()
                .filter(item -> invitationId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (invitation != null && invitation.getScheduleId() != null) {
            fetchScheduleAndSetAlarm(invitation.getScheduleId());
        }
    }

    private void fetchScheduleAndSetAlarm(String scheduleId) {
        Schedule schedule = localDataStore.schedules().findById(scheduleId);
        if (schedule != null && schedule.getStartTime() != null) {
            long nowMillis = System.currentTimeMillis();
            long triggerTime = schedule.getStartTime().getTime();
            if (triggerTime > nowMillis && schedule.getAlarmOption() != null && schedule.getAlarmOption() > 0) {
                AlarmScheduler.scheduleAlarm(NotificationsActivity.this, schedule);
                Toast.makeText(NotificationsActivity.this, "Đã thêm lịch và đặt báo thức!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDeclineClick(Notification notification, int position) {
        updateInvitationStatus(notification, 2);
        sendResponseNotification(notification, "đã từ chối");
        deleteNotification(notification, position);
    }

    @Override
    public void onItemClick(Notification notification, int position) {
        String type = notification.getType();
        if (type != null && (type.equals("schedule_invitation") || type.equals("note_invitation"))) {
            return;
        }

        if (!notification.isRead()) {
            localDataStore.notifications().updateRead(notification.getId(), true);
            loadNotifications();
        }
    }

    private void sendResponseNotification(Notification originalNotification, String responseAction) {
        String invitationId = originalNotification.getReferenceId();
        if (invitationId == null) return;
        if (currentUsername == null) currentUsername = "Một người dùng"; // Đảm bảo username không null

        String title = originalNotification.getType().equals("schedule_invitation")
                ? localDataStore.scheduleInvitations().getByScheduleId(invitationId).stream().findFirst().map(ScheduleInvitation::getScheduleTitle).orElse("")
                : localDataStore.noteInvitations().getByNoteId(invitationId).stream().findFirst().map(NoteInvitation::getNoteTitle).orElse("");

        String hostId = originalNotification.getType().equals("schedule_invitation")
                ? localDataStore.scheduleInvitations().getByScheduleId(invitationId).stream().findFirst().map(ScheduleInvitation::getHostId).orElse(null)
                : localDataStore.noteInvitations().getByNoteId(invitationId).stream().findFirst().map(NoteInvitation::getHostId).orElse(null);

        if (hostId == null) {
            return;
        }

        Notification responseNotif = new Notification();
        responseNotif.setId(UUID.randomUUID().toString());
        responseNotif.setUserId(hostId);
        responseNotif.setType("reminder");
        responseNotif.setMessage(currentUsername + " " + responseAction + " lời mời của bạn: " + title);
        responseNotif.setRead(false);
        responseNotif.setCreatedAt(new Date());
        localDataStore.notifications().insert(responseNotif);
    }

    // Hàm chung để cập nhật lời mời (schedule hoặc note)
    private void updateInvitationStatus(Notification notification, int status) {
        String invitationId = notification.getReferenceId();
        if (invitationId == null) return;

        String collectionPath;
        if (notification.getType().equals("schedule_invitation")) {
            collectionPath = "schedule_invitations";
        } else if (notification.getType().equals("note_invitation")) {
            collectionPath = "note_invitations";
        } else {
            return; // Không phải loại thông báo lời mời
        }

        if (notification.getType().equals("schedule_invitation")) {
            ScheduleInvitation invitation = localDataStore.scheduleInvitations().getByScheduleId(invitationId).stream()
                    .filter(item -> invitationId.equals(item.getId()))
                    .findFirst().orElse(null);
            if (invitation != null) {
                invitation.setStatus(status);
                localDataStore.scheduleInvitations().insert(invitation);
            }
        } else if (notification.getType().equals("note_invitation")) {
            NoteInvitation invitation = localDataStore.noteInvitations().getByNoteId(invitationId).stream()
                    .filter(item -> invitationId.equals(item.getId()))
                    .findFirst().orElse(null);
            if (invitation != null) {
                invitation.setStatus(status);
                localDataStore.noteInvitations().insert(invitation);
            }
        }
    }

    // Hàm xóa thông báo (sau khi đã xử lý)
    private void deleteNotification(Notification notification, int position) {
        localDataStore.notifications().deleteById(notification.getId());
        adapter.removeItem(position);
    }
}