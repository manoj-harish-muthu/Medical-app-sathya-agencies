package com.pharmacyerp.model;

import java.math.BigDecimal;
import java.sql.Date;

public class InventoryItem {
    private String medicineName;
    private String batchNumber;
    private Date expiryDate;
    private int currentStock;
    private int minStock;
    private BigDecimal mrp;
    private String status; // Normal, Near Expiry, Expired, Low Stock

    // Getters and Setters
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }

    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
