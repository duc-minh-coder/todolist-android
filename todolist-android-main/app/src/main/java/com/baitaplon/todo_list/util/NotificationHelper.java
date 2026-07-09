package com.baitaplon.todo_list.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.activity.MainActivity; // Mở MainActivity khi nhấn vào

public class NotificationHelper {

    private static final String CHANNEL_ID = "schedule_reminders";
    private static final String CHANNEL_NAME = "Schedule Reminders";
    private static final String CHANNEL_DESC = "Notifications for schedule reminders";

    /**
     * Tạo Notification Channel (chỉ cần gọi 1 lần)
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // Ưu tiên cao để hiện lên
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Xây dựng và hiển thị thông báo
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public static void showNotification(Context context, String title, String message, int notificationId) {
        // Tạo Intent để mở app khi nhấn vào thông báo
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications) // Icon chuông
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Ưu tiên cao
                .setContentIntent(pendingIntent) // Intent khi nhấn vào
                .setAutoCancel(true) // Tự xóa khi nhấn vào
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Hiển thị trên màn hình khóa

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Cần xin quyền POST_NOTIFICATIONS trên Android 13+ (đã làm trong Manifest)
        notificationManager.notify(notificationId, builder.build());
    }
}