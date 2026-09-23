package com.pharmacyerp.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ExpiryReportRow {
    private String medicineName;
    private String batchNumber;
    private LocalDate expiryDate;
    private String expiryDateStr;
    private int currentQuantity;
    private String companyName;
    private String status;

    public ExpiryReportRow(String medicineName, String batchNumber, LocalDate expiryDate, int currentQuantity, String companyName) {
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.currentQuantity = currentQuantity;
        this.companyName = companyName != null ? companyName : "Unknown";
        
        if (expiryDate != null) {
            this.expiryDateStr = expiryDate.format(DateTimeFormatter.ofPattern("MM/yyyy"));
            if (expiryDate.isBefore(LocalDate.now())) {
                this.status = "Expired";
            } else if (expiryDate.isBefore(LocalDate.now().plusMonths(1))) {
                this.status = "Expiring Soon";
            } else {
                this.status = "Valid";
            }
        } else {
            this.expiryDateStr = "N/A";
            this.status = "Unknown";
        }
    }

    public String getMedicineName() { return medicineName; }
    public String getBatchNumber() { return batchNumber; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getExpiryDateStr() { return expiryDateStr; }
    public int getCurrentQuantity() { return currentQuantity; }
    public String getCompanyName() { return companyName; }
    public String getStatus() { return status; }
}
