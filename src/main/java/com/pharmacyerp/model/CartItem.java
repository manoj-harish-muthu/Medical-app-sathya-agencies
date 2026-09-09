package com.pharmacyerp.model;

import java.math.BigDecimal;

public class CartItem {
    private int medicineId;
    private int batchId;
    private String medicineName;
    private String batchNumber;
    private String hsnCode;
    private BigDecimal gstRate;
    private String scheduleType;
    
    private String companyName;
    private String packing;
    private String expiryDateStr;
    
    private int quantity;
    private BigDecimal mrp;
    private BigDecimal sellingRate;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal netAmount;

    public CartItem() {
        this.quantity = 1;
        this.discountPercentage = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.cgstAmount = BigDecimal.ZERO;
        this.sgstAmount = BigDecimal.ZERO;
        this.netAmount = BigDecimal.ZERO;
    }

    public void calculateTotals() {
        if (sellingRate == null) sellingRate = BigDecimal.ZERO;
        if (discountPercentage == null) discountPercentage = BigDecimal.ZERO;
        if (gstRate == null) gstRate = BigDecimal.ZERO;

        BigDecimal grossAmount = sellingRate.multiply(new BigDecimal(quantity));
        this.discountAmount = grossAmount.multiply(discountPercentage).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal taxableAmount = grossAmount.subtract(discountAmount);
        
        // Split GST into CGST and SGST
        BigDecimal halfRate = gstRate.divide(new BigDecimal(2), 4, java.math.RoundingMode.HALF_UP);
        this.cgstAmount = taxableAmount.multiply(halfRate).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
        this.sgstAmount = taxableAmount.multiply(halfRate).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
        
        this.netAmount = taxableAmount.add(cgstAmount).add(sgstAmount).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // Getters and Setters
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { 
        this.gstRate = gstRate; 
        calculateTotals();
    }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getPacking() { return packing; }
    public void setPacking(String packing) { this.packing = packing; }

    public String getExpiryDateStr() { return expiryDateStr; }
    public void setExpiryDateStr(String expiryDateStr) { this.expiryDateStr = expiryDateStr; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
        calculateTotals();
    }

    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }

    public BigDecimal getSellingRate() { return sellingRate; }
    public void setSellingRate(BigDecimal sellingRate) { 
        this.sellingRate = sellingRate; 
        calculateTotals();
    }

    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { 
        this.discountPercentage = discountPercentage; 
        calculateTotals();
    }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getCgstAmount() { return cgstAmount; }
    public BigDecimal getSgstAmount() { return sgstAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
}
