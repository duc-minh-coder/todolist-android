package com.baitaplon.todo_list.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "note_invitations")
public class NoteInvitation {
    @PrimaryKey
    @NonNull
    private String id;
    private String noteId;
    private String hostId;
    private String invitedUserId;
    private int status; // 0=pending, 1=accepted, 2=declined
    private String noteTitle;

    public NoteInvitation() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNoteId() { return noteId; }
    public void setNoteId(String noteId) { this.noteId = noteId; }

    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    public String getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(String invitedUserId) { this.invitedUserId = invitedUserId; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getNoteTitle() { return noteTitle; }
    public void setNoteTitle(String noteTitle) { this.noteTitle = noteTitle; }
}