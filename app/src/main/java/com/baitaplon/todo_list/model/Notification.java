package com.baitaplon.todo_list.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Notification {

    @DocumentId
    private String id;

    private String userId; // Người nhận
    private String type; // "schedule_invitation", "note_invitation", "reminder"
    private String message;
    private String referenceId; // ID của invitation hoặc schedule liên quan
    private boolean isRead;

    @ServerTimestamp
    private Date createdAt; // Thời gian tạo

    public Notification() {
    }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}