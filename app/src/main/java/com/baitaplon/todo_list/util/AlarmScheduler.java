package com.baitaplon.todo_list.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.receiver.ScheduleAlarmReceiver;
import java.util.Date;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";

    /**
     * Lên lịch báo thức cho một lịch trình
     */
    public static void scheduleAlarm(Context context, Schedule schedule) {
        if (context == null || schedule == null || schedule.getId() == null) {
            Log.e(TAG, "Context or Schedule is null. Cannot schedule alarm.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Tạo Intent để gửi đến BroadcastReceiver
        Intent intent = new Intent(context, ScheduleAlarmReceiver.class);
        intent.putExtra("SCHEDULE_ID", schedule.getId());
        intent.putExtra("SCHEDULE_TITLE", schedule.getTitle());
        intent.putExtra("IS_REPEAT", schedule.getRepeat());
        intent.putExtra("ALARM_OPTION", schedule.getAlarmOption());

        // Dùng hashCode() của ID (String) làm Request Code (int) cho PendingIntent
        int requestCode = schedule.getId().hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // --- Tính toán thời gian báo thức (trigger time) ---
        long triggerAtMillis = 0;
        if (schedule.getStartTime() == null) return; // Không có thời gian, không báo thức

        long startTimeMillis = schedule.getStartTime().getTime();

        // Lấy thời gian hiện tại
        long nowMillis = System.currentTimeMillis();

        switch (schedule.getAlarmOption()) {
            case 0: // Không báo thức
                Log.d(TAG, "Hủy báo thức (Không báo thức) cho: " + schedule.getTitle());
                cancelAlarm(context, schedule); // Hủy báo thức cũ (nếu có)
                return;
            case 1: // Trước 5 phút
                triggerAtMillis = startTimeMillis - (5 * 60 * 1000);
                break;
            case 2: // Trước 10 phút
                triggerAtMillis = startTimeMillis - (10 * 60 * 1000);
                break;
            case 3: // Trước 30 phút
                triggerAtMillis = startTimeMillis - (30 * 60 * 1000);
                break;
            case 4: // Trước 1 ngày
                triggerAtMillis = startTimeMillis - (24 * 60 * 60 * 1000);
                break;
        }

        // Nếu thời gian báo thức đã ở trong quá khứ -> không đặt
        if (triggerAtMillis < nowMillis) {
            Log.w(TAG, "Thời gian báo thức đã ở trong quá khứ. Không đặt báo thức cho: " + schedule.getTitle());
            return;
        }

        // --- Đặt báo thức chính xác ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.d(TAG, "Báo thức (exact) đã đặt cho: " + schedule.getTitle() + " lúc " + new Date(triggerAtMillis));
            } else {
                // Xử lý khi không có quyền (ví dụ: chuyển người dùng đến Settings)
                Log.w(TAG, "Không có quyền SCHEDULE_EXACT_ALARM.");
                // Có thể dùng setWindow() thay thế
            }
        } else {
            // Phiên bản cũ hơn
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Log.d(TAG, "Báo thức (exact) đã đặt cho: " + schedule.getTitle() + " lúc " + new Date(triggerAtMillis));
        }
    }

    /**
     * Hủy một báo thức dựa trên Schedule
     */
    public static void cancelAlarm(Context context, Schedule schedule) {
        if (context == null || schedule == null || schedule.getId() == null) {
            Log.e(TAG, "Context or Schedule ID is null. Cannot cancel alarm.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduleAlarmReceiver.class);

        int requestCode = schedule.getId().hashCode(); // Request code phải giống hệt lúc tạo

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE // FLAG_NO_CREATE để kiểm tra
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Đã hủy báo thức cho: " + schedule.getTitle());
        } else {
            Log.d(TAG, "Không tìm thấy báo thức để hủy cho: " + schedule.getTitle());
        }
    }
}