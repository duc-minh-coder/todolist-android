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
import com.baitaplon.todo_list.adapter.NotificationAdapter;
import com.baitaplon.todo_list.model.Notification;
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.util.AlarmScheduler;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.NotificationClickListener {

    private static final String TAG = "NotificationsActivity";

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private TextView tvNoNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration notificationListener;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

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
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                    } else {
                        currentUsername = "Một người dùng";
                    }
                })
                .addOnFailureListener(e -> currentUsername = "Một người dùng");
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, notificationList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        if (notificationListener != null) {
            notificationListener.remove();
        }

        // Query: Lấy tất cả thông báo cho user này, sắp xếp theo thời gian mới nhất
        Query query = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50); // Giới hạn 50 thông báo gần nhất

        // Lắng nghe thời gian thực
        notificationListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                return;
            }

            notificationList.clear();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Notification notification = doc.toObject(Notification.class);
                    notificationList.add(notification);
                }
            }

            adapter.setData(notificationList);
            checkIfEmpty();
        });
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
        if (notificationListener != null) {
            notificationListener.remove();
        }
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

        // Bước 1: Tìm bản ghi lời mời trong 'schedule_invitations' để lấy scheduleId
        db.collection("schedule_invitations").document(invitationId).get()
                .addOnSuccessListener(invitationSnapshot -> {
                    if (invitationSnapshot.exists()) {
                        String scheduleId = invitationSnapshot.getString("scheduleId");
                        if (scheduleId != null) {
                            // Bước 2: Lấy chi tiết lịch trình từ 'schedules'
                            fetchScheduleAndSetAlarm(scheduleId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi lấy thông tin lời mời để đặt báo thức", e));
    }

    private void fetchScheduleAndSetAlarm(String scheduleId) {
        db.collection("schedules").document(scheduleId).get()
                .addOnSuccessListener(scheduleSnapshot -> {
                    if (scheduleSnapshot.exists()) {
                        Schedule schedule = scheduleSnapshot.toObject(Schedule.class);

                        // Kiểm tra xem lịch trình có hợp lệ và có cài báo thức không
                        if (schedule != null && schedule.getStartTime() != null) {
                            // Kiểm tra thời gian xem có ở tương lai không
                            long nowMillis = System.currentTimeMillis();
                            long triggerTime = schedule.getStartTime().toDate().getTime();

                            if (triggerTime > nowMillis && schedule.getAlarmOption() != null && schedule.getAlarmOption() > 0) {
                                // GỌI ALARM SCHEDULER ĐỂ CÀI BÁO THỨC TRÊN MÁY NGƯỜI ĐƯỢC MỜI
                                AlarmScheduler.scheduleAlarm(NotificationsActivity.this, schedule);
                                Toast.makeText(NotificationsActivity.this, "Đã thêm lịch và đặt báo thức!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Đã đặt báo thức cho lịch được mời: " + schedule.getTitle());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi lấy chi tiết lịch trình", e));
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
            db.collection("notifications").document(notification.getId())
                    .update("read", true)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read."));
            loadNotifications();
        }
    }

    private void sendResponseNotification(Notification originalNotification, String responseAction) {
        String invitationId = originalNotification.getReferenceId();
        if (invitationId == null) return;
        if (currentUsername == null) currentUsername = "Một người dùng"; // Đảm bảo username không null

        // Xác định collection invitation
        String collectionPath;
        if (originalNotification.getType().equals("schedule_invitation")) {
            collectionPath = "schedule_invitations";
        } else if (originalNotification.getType().equals("note_invitation")) {
            collectionPath = "note_invitations";
        } else {
            return;
        }

        // Lấy thông tin của lời mời gốc để tìm hostId và tiêu đề
        db.collection(collectionPath).document(invitationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "Invitation document not found!");
                        return;
                    }

                    // Lấy ID của người host (người đã mời mình)
                    String hostId = documentSnapshot.getString("hostId");
                    String title = documentSnapshot.contains("scheduleTitle") ?
                            documentSnapshot.getString("scheduleTitle") :
                            documentSnapshot.getString("noteTitle");

                    if (hostId == null) {
                        Log.e(TAG, "Host ID is null in invitation!");
                        return;
                    }

                    // Tạo thông báo mới
                    Notification responseNotif = new Notification();
                    responseNotif.setUserId(hostId); // Gửi cho người host
                    responseNotif.setType("reminder"); // Chỉ là thông báo nhắc nhở, không cần hành động
                    responseNotif.setMessage(currentUsername + " " + responseAction + " lời mời của bạn: " + title);
                    responseNotif.setRead(false);

                    // Gửi thông báo mới lên collection "notifications"
                    db.collection("notifications").add(responseNotif)
                            .addOnSuccessListener(documentReference -> Log.d(TAG, "Response notification sent to host: " + hostId))
                            .addOnFailureListener(e -> Log.e(TAG, "Error sending response notification", e));

                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching invitation details for response", e));
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

        // Cập nhật status
        db.collection(collectionPath).document(invitationId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Invitation status updated to " + status))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating invitation status", e));
    }

    // Hàm xóa thông báo (sau khi đã xử lý)
    private void deleteNotification(Notification notification, int position) {
        db.collection("notifications").document(notification.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification deleted.");
                     adapter.removeItem(position); // Không cần nếu dùng listener
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting notification", e));
    }
}