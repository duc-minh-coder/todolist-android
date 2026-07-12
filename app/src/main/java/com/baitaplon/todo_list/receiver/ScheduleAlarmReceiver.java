package com.baitaplon.todo_list.receiver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import androidx.annotation.RequiresPermission;

import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.util.AlarmScheduler;
import com.baitaplon.todo_list.util.NotificationHelper;

import java.util.Calendar;
import java.util.Date;

public class ScheduleAlarmReceiver extends BroadcastReceiver {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScheduleAlarmReceiver:WakeLock");
        wakeLock.acquire(10 * 1000L);

        String scheduleId = intent.getStringExtra("SCHEDULE_ID");
        String title = intent.getStringExtra("SCHEDULE_TITLE");
        boolean isRepeat = intent.getBooleanExtra("IS_REPEAT", false);
        int alarmOption = intent.getIntExtra("ALARM_OPTION", 0);

        NotificationHelper.createNotificationChannel(context);
        NotificationHelper.showNotification(context, title != null ? title : "Lịch trình", "Lịch trình của bạn sắp bắt đầu.", scheduleId != null ? scheduleId.hashCode() : (int) System.currentTimeMillis());

        if (isRepeat) {
            Schedule nextSchedule = new Schedule();
            nextSchedule.setId(scheduleId);
            nextSchedule.setTitle(title);
            nextSchedule.setRepeat(true);
            nextSchedule.setAlarmOption(alarmOption);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            nextSchedule.setStartTime(new Date(calendar.getTimeInMillis()));
            AlarmScheduler.scheduleAlarm(context, nextSchedule);
        }

        wakeLock.release();
    }
}