package com.baitaplon.todo_list.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.util.AlarmScheduler;
import com.baitaplon.todo_list.util.NotificationHelper;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;

public class ScheduleAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "ScheduleAlarmReceiver";

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Báo thức đã nhận!");

        // 1. Giữ cho CPU "thức" một lúc để xử lý
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":WakeLock");
        wakeLock.acquire(10 * 1000); // Giữ trong 10 giây

        // 2. Lấy thông tin từ Intent
        String scheduleId = intent.getStringExtra("SCHEDULE_ID");
        String title = intent.getStringExtra("SCHEDULE_TITLE");
        boolean isRepeat = intent.getBooleanExtra("IS_REPEAT", false);
        int alarmOption = intent.getIntExtra("ALARM_OPTION", 0);

        if (title == null) {
            title = "Bạn có lịch trình sắp tới!";
        }
        String message = "Lịch trình của bạn sắp bắt đầu.";

        // 3. Tạo Notification Channel (gọi nhiều lần không sao)
        NotificationHelper.createNotificationChannel(context);

        // 4. Hiển thị thông báo
        int notificationId = scheduleId != null ? scheduleId.hashCode() : (int) System.currentTimeMillis();
        NotificationHelper.showNotification(context, title, message, notificationId);

        // 5. [XỬ LÝ LẶP LẠI]
        if (isRepeat) {
            Log.d(TAG, "Xử lý lặp lại cho: " + title);
            // Giả sử lặp lại HÀNG NGÀY
            // Tạo một đối tượng Schedule "ảo" cho ngày hôm sau và đặt báo thức mới

            Schedule nextSchedule = new Schedule();
            nextSchedule.setId(scheduleId); // ID phải giống hệt
            nextSchedule.setTitle(title);
            nextSchedule.setRepeat(true);
            nextSchedule.setAlarmOption(alarmOption);

            // Tính thời gian bắt đầu của ngày mai
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1); // Thêm 1 ngày
            Date nextStartTime = cal.getTime();

            nextSchedule.setStartTime(new Timestamp(nextStartTime));

            // Lên lịch báo thức mới cho ngày mai
            AlarmScheduler.scheduleAlarm(context, nextSchedule);
        }

        // 6. Nhả WakeLock
        wakeLock.release();
    }
}