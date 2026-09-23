package com.pharmacyerp.model;

import java.time.LocalDateTime;

public class Schedule {
    private int scheduleId;
    private String scheduleName;
    private boolean requiresPrescription;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Schedule() {}

    public Schedule(int scheduleId, String scheduleName, boolean requiresPrescription, String description) {
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.requiresPrescription = requiresPrescription;
        this.description = description;
    }

    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public String getScheduleName() { return scheduleName; }
    public void setScheduleName(String scheduleName) { this.scheduleName = scheduleName; }

    public boolean isRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(boolean requiresPrescription) { this.requiresPrescription = requiresPrescription; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
