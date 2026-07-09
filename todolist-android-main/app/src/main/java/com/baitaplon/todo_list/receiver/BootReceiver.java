package com.baitaplon.todo_list.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.model.ScheduleInvitation;
import com.baitaplon.todo_list.util.AlarmScheduler;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Điện thoại vừa khởi động xong. Đang đặt lại báo thức...");

            // Lấy User ID đã lưu
            SharedPreferences sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
            String currentUserId = sharedPreferences.getString("current_user_id", null);

            if (currentUserId == null) {
                Log.w(TAG, "Không tìm thấy User ID, không thể đặt lại báo thức.");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            long nowMillis = System.currentTimeMillis();

            // Lấy tất cả lịch trình (chưa hoàn thành) của user đó trong tương lai
            db.collection("schedules")
                    .whereEqualTo("hostId", currentUserId) // Lịch tôi tạo
                    .whereEqualTo("completed", false)
                    .whereGreaterThan("startTime", new Timestamp(new Date(nowMillis))) // Chỉ lấy lịch trong tương lai
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Schedule schedule = doc.toObject(Schedule.class);
                            // schedule.setId(doc.getId()); // Model đã có @DocumentId

                            // Đặt lại báo thức cho từng lịch
                            if (schedule.getAlarmOption() > 0) { // Chỉ đặt nếu có cài báo thức
                                AlarmScheduler.scheduleAlarm(context, schedule);
                            }
                        }
                        Log.d(TAG, "Đã đặt lại báo thức cho " + snapshots.size() + " lịch (hosted).");
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi lấy hosted schedules lúc khởi động", e));

            // --- Xử lý các lịch trình được mời (Invited Schedules) ---
            // Truy vấn bảng schedule_invitations để tìm các lời mời:
            // 1. Gửi tới user hiện tại (invitedUserId == currentUserId)
            // 2. Đã chấp nhận (status == 1)
            // 3. Chưa hoàn thành (completed == false)
            db.collection("schedule_invitations")
                    .whereEqualTo("invitedUserId", currentUserId)
                    .whereEqualTo("status", 1)
                    .whereEqualTo("completed", false)
                    .get()
                    .addOnSuccessListener(invitationSnapshots -> {
                        for (QueryDocumentSnapshot doc : invitationSnapshots) {
                            ScheduleInvitation invitation = doc.toObject(ScheduleInvitation.class);
                            String scheduleId = invitation.getScheduleId();

                            if (scheduleId != null) {
                                // Lấy chi tiết lịch trình gốc để biết thời gian và tùy chọn báo thức
                                db.collection("schedules").document(scheduleId)
                                        .get()
                                        .addOnSuccessListener(scheduleSnapshot -> {
                                            if (scheduleSnapshot.exists()) {
                                                Schedule schedule = scheduleSnapshot.toObject(Schedule.class);

                                                // Kiểm tra schedule hợp lệ và có thời gian bắt đầu
                                                if (schedule != null && schedule.getStartTime() != null) {
                                                    long scheduleTime = schedule.getStartTime().toDate().getTime();

                                                    // Chỉ đặt báo thức nếu lịch ở trong tương lai
                                                    if (scheduleTime > nowMillis) {
                                                        // Kiểm tra xem lịch này có bật báo thức không (AlarmOption > 0)
                                                        if (schedule.getAlarmOption() != null && schedule.getAlarmOption() > 0) {
                                                            AlarmScheduler.scheduleAlarm(context, schedule);
                                                            Log.d(TAG, "Đã đặt lại báo thức (invited): " + schedule.getTitle());
                                                        }
                                                    }
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi lấy chi tiết lịch mời: " + scheduleId, e));
                            }
                        }
                        Log.d(TAG, "Đã kiểm tra " + invitationSnapshots.size() + " lời mời để đặt lại báo thức.");
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi lấy schedule_invitations lúc khởi động", e));
        }
    }
}