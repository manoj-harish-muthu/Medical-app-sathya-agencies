package com.pharmacyerp.model;

import java.time.LocalDateTime;

public class Supplier {
    private int supplierId;
    private String supplierName;
    private String gstin;
    private String dlNumber;
    private String phone;
    private String email;
    private String address;
    private double openingBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getDlNumber() { return dlNumber; }
    public void setDlNumber(String dlNumber) { this.dlNumber = dlNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return supplierName != null ? supplierName : "Unknown Supplier";
    }
}
