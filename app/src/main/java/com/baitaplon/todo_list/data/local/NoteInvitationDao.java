package com.baitaplon.todo_list.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.baitaplon.todo_list.model.NoteInvitation;

import java.util.List;

@Dao
public interface NoteInvitationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NoteInvitation invitation);

    @Query("SELECT * FROM note_invitations WHERE noteId = :noteId AND invitedUserId = :invitedUserId LIMIT 1")
    NoteInvitation findByNoteAndUser(String noteId, String invitedUserId);

    @Query("SELECT * FROM note_invitations WHERE invitedUserId = :userId AND status = :status")
    List<NoteInvitation> getByUserAndStatus(String userId, int status);

    @Query("SELECT * FROM note_invitations WHERE noteId = :noteId")
    List<NoteInvitation> getByNoteId(String noteId);

    @Query("DELETE FROM note_invitations WHERE noteId = :noteId")
    void deleteByNoteId(String noteId);

    @Query("DELETE FROM note_invitations WHERE noteId = :noteId AND invitedUserId = :invitedUserId")
    void deleteByNoteAndUser(String noteId, String invitedUserId);

    @Delete
    void delete(NoteInvitation invitation);
}