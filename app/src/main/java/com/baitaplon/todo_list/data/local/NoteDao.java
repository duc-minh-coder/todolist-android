package com.baitaplon.todo_list.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.baitaplon.todo_list.model.Note;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note findById(String id);

    @Query("SELECT * FROM notes WHERE creatorId = :creatorId ORDER BY COALESCE(lastEdited, createdAt) DESC")
    List<Note> getNotesByCreator(String creatorId);

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY isPinned DESC, COALESCE(lastEdited, createdAt) DESC")
    List<Note> search(String query);

    @Query("UPDATE notes SET title = :title, content = :content, isPinned = :pinned, lastEdited = :lastEdited, lastEditedBy = :lastEditedBy WHERE id = :id")
    void updateNote(String id, String title, String content, boolean pinned, Long lastEdited, String lastEditedBy);

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    void updatePinned(String id, boolean pinned);

    @Delete
    void delete(Note note);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(String id);
}