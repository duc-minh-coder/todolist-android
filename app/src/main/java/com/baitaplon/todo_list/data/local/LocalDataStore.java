package com.baitaplon.todo_list.data.local;

import android.content.Context;

public class LocalDataStore {

    private static volatile LocalDataStore instance;

    private final AppDatabase database;

    private LocalDataStore(Context context) {
        database = AppDatabase.getInstance(context);
    }

    public static LocalDataStore getInstance(Context context) {
        if (instance == null) {
            synchronized (LocalDataStore.class) {
                if (instance == null) {
                    instance = new LocalDataStore(context);
                }
            }
        }
        return instance;
    }

    public UserDao users() {
        return database.userDao();
    }

    public NoteDao notes() {
        return database.noteDao();
    }

    public NoteVersionDao noteVersions() {
        return database.noteVersionDao();
    }

    public NoteInvitationDao noteInvitations() {
        return database.noteInvitationDao();
    }

    public ScheduleDao schedules() {
        return database.scheduleDao();
    }

    public ScheduleInvitationDao scheduleInvitations() {
        return database.scheduleInvitationDao();
    }

    public NotificationDao notifications() {
        return database.notificationDao();
    }
}