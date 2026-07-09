package com.baitaplon.todo_list.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class NoteVersion {
    private String id;
    private String noteId;
    private String title;
    private String content;
    private @ServerTimestamp Timestamp editedAt;
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

    public Timestamp getEditedAt() { return editedAt; }
    public void setEditedAt(Timestamp editedAt) { this.editedAt = editedAt; }

    public String getEditorId() { return editorId; }
    public void setEditorId(String editorId) { this.editorId = editorId; }

    public String getEditorName() { return editorName; }
    public void setEditorName(String editorName) { this.editorName = editorName; }
}