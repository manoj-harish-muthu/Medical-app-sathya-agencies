package com.pharmacyerp.model;

import java.math.BigDecimal;

public class GstSummaryRow {
    private String taxPercentage;
    private BigDecimal taxableValue;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal totalAmount;

    public GstSummaryRow(String taxPercentage, BigDecimal taxableValue, BigDecimal cgstAmount, BigDecimal sgstAmount, BigDecimal totalAmount) {
        this.taxPercentage = taxPercentage;
        this.taxableValue = taxableValue;
        this.cgstAmount = cgstAmount;
        this.sgstAmount = sgstAmount;
        this.totalAmount = totalAmount;
    }

    public String getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(String taxPercentage) { this.taxPercentage = taxPercentage; }

    public BigDecimal getTaxableValue() { return taxableValue; }
    public void setTaxableValue(BigDecimal taxableValue) { this.taxableValue = taxableValue; }

    public BigDecimal getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(BigDecimal cgstAmount) { this.cgstAmount = cgstAmount; }

    public BigDecimal getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(BigDecimal sgstAmount) { this.sgstAmount = sgstAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
