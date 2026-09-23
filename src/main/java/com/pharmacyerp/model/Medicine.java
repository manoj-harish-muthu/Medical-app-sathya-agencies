package com.pharmacyerp.model;

import java.time.LocalDateTime;

public class Medicine {
    private int medicineId;
    private String medicineName;
    private String saltName;
    private int companyId;
    private int categoryId;
    private int brandId;
    private int scheduleId;
    private String hsnCode;
    private double gstRate;
    private boolean prescriptionRequired;
    private String scheduleType;
    private String packSize;
    private int minimumStock;
    private int maximumStock;
    private int reorderLevel;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String packing;
    private String unit1st;
    private String unit2nd;
    private String decimalAllowed;
    private String colorType;
    private String itemType;
    private double cgst;
    private double sgst;
    private double igst;
    private String localTaxType;
    private String centralTaxType;
    private String negativeAllowed;
    
    // Batch specific fields needed for the unified UI save
    private String batchNumber;
    private java.time.LocalDate expiryDate;
    private int quantity;
    private double purchaseRate;
    private double mrp;
    private double sellingRate;
    private double rateA;
    private double rateB;
    private double rateC;
    private double costPerPcs;
    private int convStr;
    private int convCas;

    // Additional transient fields for UI/Joins
    private String categoryName;
    private String companyName;
    private String brandName;
    private String scheduleName;
    private int totalStock;
    private double currentMrp;

    // Getters and Setters
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public int getBrandId() { return brandId; }
    public void setBrandId(int brandId) { this.brandId = brandId; }

    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getScheduleName() { return scheduleName; }
    public void setScheduleName(String scheduleName) { this.scheduleName = scheduleName; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getSaltName() { return saltName; }
    public void setSaltName(String saltName) { this.saltName = saltName; }

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }

    public double getGstRate() { return gstRate; }
    public void setGstRate(double gstRate) { this.gstRate = gstRate; }

    public boolean isPrescriptionRequired() { return prescriptionRequired; }
    public void setPrescriptionRequired(boolean prescriptionRequired) { this.prescriptionRequired = prescriptionRequired; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public String getPackSize() { return packSize; }
    public void setPackSize(String packSize) { this.packSize = packSize; }

    public int getMinimumStock() { return minimumStock; }
    public void setMinimumStock(int minimumStock) { this.minimumStock = minimumStock; }

    public int getMaximumStock() { return maximumStock; }
    public void setMaximumStock(int maximumStock) { this.maximumStock = maximumStock; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }

    public double getCurrentMrp() { return currentMrp; }
    public void setCurrentMrp(double currentMrp) { this.currentMrp = currentMrp; }

    public String getPacking() { return packing; }
    public void setPacking(String packing) { this.packing = packing; }
    
    public String getUnit1st() { return unit1st; }
    public void setUnit1st(String unit1st) { this.unit1st = unit1st; }
    
    public String getUnit2nd() { return unit2nd; }
    public void setUnit2nd(String unit2nd) { this.unit2nd = unit2nd; }
    
    public String getDecimalAllowed() { return decimalAllowed; }
    public void setDecimalAllowed(String decimalAllowed) { this.decimalAllowed = decimalAllowed; }
    
    public String getColorType() { return colorType; }
    public void setColorType(String colorType) { this.colorType = colorType; }
    
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    
    public double getCgst() { return cgst; }
    public void setCgst(double cgst) { this.cgst = cgst; }
    
    public double getSgst() { return sgst; }
    public void setSgst(double sgst) { this.sgst = sgst; }
    
    public double getIgst() { return igst; }
    public void setIgst(double igst) { this.igst = igst; }
    
    public String getLocalTaxType() { return localTaxType; }
    public void setLocalTaxType(String localTaxType) { this.localTaxType = localTaxType; }
    
    public String getCentralTaxType() { return centralTaxType; }
    public void setCentralTaxType(String centralTaxType) { this.centralTaxType = centralTaxType; }
    
    public String getNegativeAllowed() { return negativeAllowed; }
    public void setNegativeAllowed(String negativeAllowed) { this.negativeAllowed = negativeAllowed; }
    
    public double getRateA() { return rateA; }
    public void setRateA(double rateA) { this.rateA = rateA; }
    
    public double getRateB() { return rateB; }
    public void setRateB(double rateB) { this.rateB = rateB; }
    
    public double getRateC() { return rateC; }
    public void setRateC(double rateC) { this.rateC = rateC; }
    
    public double getCostPerPcs() { return costPerPcs; }
    public void setCostPerPcs(double costPerPcs) { this.costPerPcs = costPerPcs; }
    
    public int getConvStr() { return convStr; }
    public void setConvStr(int convStr) { this.convStr = convStr; }
    
    public int getConvCas() { return convCas; }
    public void setConvCas(int convCas) { this.convCas = convCas; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    
    public java.time.LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(java.time.LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }
    
    public double getMrp() { return mrp; }
    public void setMrp(double mrp) { this.mrp = mrp; }
    
    public double getSellingRate() { return sellingRate; }
    public void setSellingRate(double sellingRate) { this.sellingRate = sellingRate; }
}
