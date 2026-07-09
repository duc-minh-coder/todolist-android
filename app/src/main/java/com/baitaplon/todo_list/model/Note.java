package com.baitaplon.todo_list.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.List;

public class Note {
    private String id;
    private String title;
    private String content;
    private String creatorId;
    private @ServerTimestamp Timestamp createdAt;
    private @ServerTimestamp Timestamp lastEdited;

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

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getLastEdited() { return lastEdited; }
    public void setLastEdited(Timestamp lastEdited) { this.lastEdited = lastEdited; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public String getLastEditedBy() { return lastEditedBy; }
    public void setLastEditedBy(String lastEditedBy) { this.lastEditedBy = lastEditedBy; }

    public List<String> getInvitedUserIds() { return invitedUserIds; }
    public void setInvitedUserIds(List<String> invitedUserIds) { this.invitedUserIds = invitedUserIds; }
}