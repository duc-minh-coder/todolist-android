package com.baitaplon.todo_list.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.model.ScheduleInvitation;
import com.baitaplon.todo_list.util.AlarmScheduler;

import java.util.List;

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

            LocalDataStore localDataStore = LocalDataStore.getInstance(context);
            long nowMillis = System.currentTimeMillis();

            List<Schedule> hostedSchedules = localDataStore.schedules().getByHostId(currentUserId);
            for (Schedule schedule : hostedSchedules) {
                if (schedule.getStartTime() != null && schedule.getStartTime().getTime() > nowMillis && schedule.getAlarmOption() != null && schedule.getAlarmOption() > 0) {
                    AlarmScheduler.scheduleAlarm(context, schedule);
                }
            }

            List<ScheduleInvitation> invitations = localDataStore.scheduleInvitations().getAcceptedIncomplete(currentUserId, 1, false);
            for (ScheduleInvitation invitation : invitations) {
                if (invitation.getScheduleId() == null) continue;
                Schedule schedule = localDataStore.schedules().findById(invitation.getScheduleId());
                if (schedule != null && schedule.getStartTime() != null && schedule.getStartTime().getTime() > nowMillis && schedule.getAlarmOption() != null && schedule.getAlarmOption() > 0) {
                    AlarmScheduler.scheduleAlarm(context, schedule);
                }
            }
        }
    }
}