package com.pharmacyerp.model;

import java.time.LocalDateTime;

public class GenericSalt {
    private int saltId;
    private String saltName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GenericSalt() {}

    public GenericSalt(int saltId, String saltName, String description) {
        this.saltId = saltId;
        this.saltName = saltName;
        this.description = description;
    }

    public int getSaltId() { return saltId; }
    public void setSaltId(int saltId) { this.saltId = saltId; }

    public String getSaltName() { return saltName; }
    public void setSaltName(String saltName) { this.saltName = saltName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
