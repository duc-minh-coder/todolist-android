package com.baitaplon.todo_list.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.baitaplon.todo_list.model.ScheduleInvitation;

import java.util.List;

@Dao
public interface ScheduleInvitationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ScheduleInvitation invitation);

    @Query("SELECT * FROM schedule_invitations WHERE scheduleId = :scheduleId AND invitedUserId = :invitedUserId LIMIT 1")
    ScheduleInvitation findByScheduleAndUser(String scheduleId, String invitedUserId);

    @Query("SELECT * FROM schedule_invitations WHERE invitedUserId = :userId AND status = :status AND isCompleted = :completed")
    List<ScheduleInvitation> getAcceptedIncomplete(String userId, int status, boolean completed);

    @Query("SELECT * FROM schedule_invitations WHERE scheduleId = :scheduleId")
    List<ScheduleInvitation> getByScheduleId(String scheduleId);

    @Query("DELETE FROM schedule_invitations WHERE scheduleId = :scheduleId")
    void deleteByScheduleId(String scheduleId);

    @Query("DELETE FROM schedule_invitations WHERE scheduleId = :scheduleId AND invitedUserId = :invitedUserId")
    void deleteByScheduleAndUser(String scheduleId, String invitedUserId);

    @Delete
    void delete(ScheduleInvitation invitation);
}