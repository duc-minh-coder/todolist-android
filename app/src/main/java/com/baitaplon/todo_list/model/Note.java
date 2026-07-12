package com.baitaplon.todo_list.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import java.util.Date;
import java.util.List;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String content;
    private String creatorId;
    private Date createdAt;
    private Date lastEdited;

    private boolean isPinned;
    private String lastEditedBy; // Tên người chỉnh sửa cuối cùng
    private List<String> invitedUserIds; // Danh sách ID người được chia sẻ

    public Note() {
    }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastEdited() { return lastEdited; }
    public void setLastEdited(Date lastEdited) { this.lastEdited = lastEdited; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public String getLastEditedBy() { return lastEditedBy; }
    public void setLastEditedBy(String lastEditedBy) { this.lastEditedBy = lastEditedBy; }

    public List<String> getInvitedUserIds() { return invitedUserIds; }
    public void setInvitedUserIds(List<String> invitedUserIds) { this.invitedUserIds = invitedUserIds; }
}