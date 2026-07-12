package com.baitaplon.todo_list.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.baitaplon.todo_list.model.Notification;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Notification notification);

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    List<Notification> getByUser(String userId);

    @Query("SELECT * FROM notifications WHERE userId = :userId AND isRead = 0")
    List<Notification> getUnreadByUser(String userId);

    @Query("UPDATE notifications SET isRead = :read WHERE id = :id")
    void updateRead(String id, boolean read);

    @Query("DELETE FROM notifications WHERE id = :id")
    void deleteById(String id);

    @Delete
    void delete(Notification notification);
}