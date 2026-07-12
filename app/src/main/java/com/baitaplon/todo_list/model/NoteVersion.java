package com.baitaplon.todo_list.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.util.Date;

@Entity(tableName = "note_versions")
public class NoteVersion {
    @PrimaryKey
    @NonNull
    private String id;
    private String noteId;
    private String title;
    private String content;
    private Date editedAt;
    private String editorId;
    private String editorName;

    public NoteVersion() {}

    public NoteVersion(String noteId, String title, String content, String editorId, String editorName) {
        this.noteId = noteId;
        this.title = title;
        this.content = content;
        this.editorId = editorId;
        this.editorName = editorName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNoteId() { return noteId; }
    public void setNoteId(String noteId) { this.noteId = noteId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getEditedAt() { return editedAt; }
    public void setEditedAt(Date editedAt) { this.editedAt = editedAt; }

    public String getEditorId() { return editorId; }
    public void setEditorId(String editorId) { this.editorId = editorId; }

    public String getEditorName() { return editorName; }
    public void setEditorName(String editorName) { this.editorName = editorName; }
}