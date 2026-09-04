package com.pharmacyerp.model;

import java.math.BigDecimal;

public class DailySummary {
    private BigDecimal cashSales = BigDecimal.ZERO;
    private BigDecimal upiSales = BigDecimal.ZERO;
    private BigDecimal cardSales = BigDecimal.ZERO;
    private BigDecimal creditSales = BigDecimal.ZERO;
    
    private int totalInvoices = 0;
    private BigDecimal totalSales = BigDecimal.ZERO;

    public BigDecimal getCashSales() { return cashSales; }
    public void setCashSales(BigDecimal cashSales) { this.cashSales = cashSales; }

    public BigDecimal getUpiSales() { return upiSales; }
    public void setUpiSales(BigDecimal upiSales) { this.upiSales = upiSales; }

    public BigDecimal getCardSales() { return cardSales; }
    public void setCardSales(BigDecimal cardSales) { this.cardSales = cardSales; }

    public BigDecimal getCreditSales() { return creditSales; }
    public void setCreditSales(BigDecimal creditSales) { this.creditSales = creditSales; }

    public int getTotalInvoices() { return totalInvoices; }
    public void setTotalInvoices(int totalInvoices) { this.totalInvoices = totalInvoices; }

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }
}
