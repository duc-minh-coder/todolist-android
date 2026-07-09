package com.baitaplon.todo_list.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Schedule {
    @DocumentId
    private String id;
    private String hostId;
    private String hostUsername;
    private String title;
    private Timestamp startTime;
    private Timestamp endTime;
    private String place;
    private String notes;
    private Boolean isRepeat;
    private Integer alarmOption;
    private List<String> invitedUserIds;
    @Exclude
    private String myInvitationId;
    @Exclude
    private Boolean myCompletedStatus;

    public Schedule() {
    }

    @Exclude
    public String getTimeFromToString() {
        if (startTime == null) {
            return "N/A";
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateTimeShortFormatter = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());

        Date startDateObj = startTime.toDate();
        Date endDateObj = (endTime != null) ? endTime.toDate() : null;

        String startDateStr = dateFormatter.format(startDateObj);
        String startTimeStr = timeFormatter.format(startDateObj);

        String endDateStr = (endDateObj != null) ? dateFormatter.format(endDateObj) : null;
        String endTimeStr = (endDateObj != null) ? timeFormatter.format(endDateObj) : null;

        if (endDateObj == null || startDateStr.equals(endDateStr)) {
            Calendar calStart = Calendar.getInstance();
            calStart.setTime(startDateObj);
            boolean isStartAtMidnight = calStart.get(Calendar.HOUR_OF_DAY) == 0 && calStart.get(Calendar.MINUTE) == 0;

            if (endDateObj != null) {
                Calendar calEnd = Calendar.getInstance();
                calEnd.setTime(endDateObj);
                boolean isEndAtMidnight = calEnd.get(Calendar.HOUR_OF_DAY) == 0 && calEnd.get(Calendar.MINUTE) == 0;

                if (isStartAtMidnight && isEndAtMidnight && startDateStr.equals(endDateStr)) {
                    return "Cả ngày";
                }

                if (startTimeStr.equals(endTimeStr)) {
                    return startTimeStr;
                } else {
                    return startTimeStr + " - " + endTimeStr;
                }
            } else {
                return startTimeStr;
            }
        } else {
            String startDateTimeShort = dateTimeShortFormatter.format(startDateObj);
            String endDateTimeShort = (endDateObj != null) ? dateTimeShortFormatter.format(endDateObj) : "?";
            return startDateTimeShort + " - " + endDateTimeShort;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    public String getHostUsername() { return hostUsername; }
    public void setHostUsername(String hostUsername) { this.hostUsername = hostUsername; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getRepeat() { return isRepeat; }
    public void setRepeat(Boolean repeat) { isRepeat = repeat; }

    public Integer getAlarmOption() { return alarmOption; }
    public void setAlarmOption(Integer alarmOption) { this.alarmOption = alarmOption; }

    public List<String> getInvitedUserIds() { return invitedUserIds; }
    public void setInvitedUserIds(List<String> invitedUserIds) { this.invitedUserIds = invitedUserIds; }

    @Exclude
    public String getMyInvitationId() { return myInvitationId; }
    public void setMyInvitationId(String myInvitationId) { this.myInvitationId = myInvitationId; }

    @Exclude
    public Boolean isMyCompletedStatus() { return myCompletedStatus; }
    public void setMyCompletedStatus(Boolean myCompletedStatus) { this.myCompletedStatus = myCompletedStatus; }
}