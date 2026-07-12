package com.baitaplon.todo_list.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.baitaplon.todo_list.model.Note;
import com.baitaplon.todo_list.model.NoteInvitation;
import com.baitaplon.todo_list.model.NoteVersion;
import com.baitaplon.todo_list.model.Notification;
import com.baitaplon.todo_list.model.Schedule;
import com.baitaplon.todo_list.model.ScheduleInvitation;
import com.baitaplon.todo_list.model.User;

@Database(
    entities = {User.class, Note.class, NoteVersion.class, NoteInvitation.class, Schedule.class, ScheduleInvitation.class, Notification.class},
    version = 1,
    exportSchema = false)
@TypeConverters({AppTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract UserDao userDao();
    public abstract NoteDao noteDao();
    public abstract NoteVersionDao noteVersionDao();
    public abstract NoteInvitationDao noteInvitationDao();
    public abstract ScheduleDao scheduleDao();
    public abstract ScheduleInvitationDao scheduleInvitationDao();
    public abstract NotificationDao notificationDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "btl_local.db")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}