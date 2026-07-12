package com.baitaplon.todo_list.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.baitaplon.todo_list.model.Schedule;

import java.util.List;

@Dao
public interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Schedule schedule);

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    Schedule findById(String id);

    @Query("SELECT * FROM schedules WHERE hostId = :hostId ORDER BY startTime DESC")
    List<Schedule> getByHostId(String hostId);

    @Query("SELECT * FROM schedules WHERE startTime BETWEEN :startOfDay AND :endOfDay ORDER BY startTime ASC")
    List<Schedule> getBetween(long startOfDay, long endOfDay);

    @Query("UPDATE schedules SET hostUsername = :hostUsername, title = :title, startTime = :startTime, endTime = :endTime, place = :place, notes = :notes, isRepeat = :isRepeat, alarmOption = :alarmOption, invitedUserIds = :invitedUserIds WHERE id = :id")
    void updateSchedule(String id, String hostUsername, String title, Long startTime, Long endTime, String place, String notes, Boolean isRepeat, Integer alarmOption, List<String> invitedUserIds);

    @Query("DELETE FROM schedules WHERE id = :id")
    void deleteById(String id);

    @Delete
    void delete(Schedule schedule);
}