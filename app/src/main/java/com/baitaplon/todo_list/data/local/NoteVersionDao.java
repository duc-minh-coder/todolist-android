package com.baitaplon.todo_list.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.baitaplon.todo_list.model.NoteVersion;

import java.util.List;

@Dao
public interface NoteVersionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NoteVersion version);

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY editedAt DESC")
    List<NoteVersion> getByNoteId(String noteId);

    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    void deleteByNoteId(String noteId);

    @Delete
    void delete(NoteVersion version);
}