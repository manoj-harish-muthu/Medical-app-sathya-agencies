package com.pharmacyerp.model;

import java.time.LocalDateTime;

public class Purchase {
    private int purchaseId;
    private String invoiceNo;
    private LocalDateTime purchaseDate;
    private int supplierId;
    private String supplierName;
    private String paymentMode;
    private double subtotal;
    private double totalTax;
    private double totalDiscount;
    private double grandTotal;
    private String status;

    public Purchase() {}

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getTotalTax() { return totalTax; }
    public void setTotalTax(double totalTax) { this.totalTax = totalTax; }

    public double getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(double totalDiscount) { this.totalDiscount = totalDiscount; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
