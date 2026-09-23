package com.pharmacyerp.model;

import java.time.LocalDate;

public class PurchaseItem {
    private int purchaseItemId;
    private int purchaseId;
    private int medicineId;
    private String medicineName; // Used for UI display
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private int quantity;
    private int freeQuantity;
    private double purchaseRate;
    private double mrp;
    private double sellingRate;
    private double discountPercentage;
    private double taxPercentage;
    private double taxAmount;
    private double netAmount;

    public PurchaseItem() {}

    public int getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(int purchaseItemId) { this.purchaseItemId = purchaseItemId; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getFreeQuantity() { return freeQuantity; }
    public void setFreeQuantity(int freeQuantity) { this.freeQuantity = freeQuantity; }

    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }

    public double getMrp() { return mrp; }
    public void setMrp(double mrp) { this.mrp = mrp; }

    public double getSellingRate() { return sellingRate; }
    public void setSellingRate(double sellingRate) { this.sellingRate = sellingRate; }

    public double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(double discountPercentage) { this.discountPercentage = discountPercentage; }

    public double getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(double taxPercentage) { this.taxPercentage = taxPercentage; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public double getNetAmount() { return netAmount; }
    public void setNetAmount(double netAmount) { this.netAmount = netAmount; }
    
    public double getTotalValueBeforeTax() {
        return quantity * purchaseRate;
    }
}
