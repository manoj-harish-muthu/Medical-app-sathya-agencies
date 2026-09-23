package com.pharmacyerp.model;

import java.time.LocalDateTime;

public class Brand {
    private int brandId;
    private String brandName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Brand() {}

    public Brand(int brandId, String brandName, String description) {
        this.brandId = brandId;
        this.brandName = brandName;
        this.description = description;
    }

    public int getBrandId() { return brandId; }
    public void setBrandId(int brandId) { this.brandId = brandId; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
