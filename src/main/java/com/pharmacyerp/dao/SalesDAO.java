package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Sale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SalesDAO {
    private static final Logger logger = LoggerFactory.getLogger(SalesDAO.class);

    public List<Sale> getAllSales() {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY sale_date DESC LIMIT 100";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Sale sale = new Sale();
                sale.setSaleId(rs.getInt("sale_id"));
                sale.setInvoiceNo(rs.getString("invoice_no"));
                sale.setSaleDate(rs.getTimestamp("sale_date"));
                sale.setCustomerName(rs.getString("customer_name"));
                sale.setCustomerPhone(rs.getString("customer_phone"));
                sale.setDoctorName(rs.getString("doctor_name"));
                sale.setPaymentMode(rs.getString("payment_mode"));
                sale.setGrandTotal(rs.getBigDecimal("grand_total"));
                sale.setStatus(rs.getString("status"));
                sales.add(sale);
            }
        } catch (Exception e) {
            logger.error("Error fetching sales", e);
        }
        return sales;
    }

    public List<Sale> getDailySales(java.time.LocalDate date) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE DATE(sale_date) = ? ORDER BY sale_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, date.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Sale sale = new Sale();
                    sale.setSaleId(rs.getInt("sale_id"));
                    sale.setInvoiceNo(rs.getString("invoice_no"));
                    sale.setSaleDate(rs.getTimestamp("sale_date"));
                    sale.setCustomerName(rs.getString("customer_name"));
                    sale.setCustomerPhone(rs.getString("customer_phone"));
                    sale.setDoctorName(rs.getString("doctor_name"));
                    sale.setPaymentMode(rs.getString("payment_mode"));
                    sale.setGrandTotal(rs.getBigDecimal("grand_total"));
                    sale.setStatus(rs.getString("status"));
                    sales.add(sale);
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching daily sales", e);
        }
        return sales;
    }

    public com.pharmacyerp.model.DailySummary getDailySummary(java.time.LocalDate date) {
        com.pharmacyerp.model.DailySummary summary = new com.pharmacyerp.model.DailySummary();
        String sql = "SELECT payment_mode, COUNT(sale_id) as invoices, SUM(grand_total) as amount " +
                     "FROM sales WHERE DATE(sale_date) = ? AND status = 'COMPLETED' " +
                     "GROUP BY payment_mode";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, date.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                int totalInvoices = 0;
                java.math.BigDecimal totalSales = java.math.BigDecimal.ZERO;
                
                while (rs.next()) {
                    String mode = rs.getString("payment_mode");
                    int count = rs.getInt("invoices");
                    java.math.BigDecimal amount = rs.getBigDecimal("amount");
                    
                    if (amount == null) amount = java.math.BigDecimal.ZERO;
                    
                    totalInvoices += count;
                    totalSales = totalSales.add(amount);
                    
                    if ("Cash".equalsIgnoreCase(mode)) {
                        summary.setCashSales(amount);
                    } else if ("UPI".equalsIgnoreCase(mode)) {
                        summary.setUpiSales(amount);
                    } else if ("Card".equalsIgnoreCase(mode)) {
                        summary.setCardSales(amount);
                    } else if ("Credit".equalsIgnoreCase(mode)) {
                        summary.setCreditSales(amount);
                    }
                }
                
                summary.setTotalInvoices(totalInvoices);
                summary.setTotalSales(totalSales);
            }
        } catch (Exception e) {
            logger.error("Error fetching daily summary", e);
        }
        return summary;
    }

    public java.util.List<com.pharmacyerp.model.CartItem> getSaleItemsBySaleId(int saleId) {
        java.util.List<com.pharmacyerp.model.CartItem> items = new java.util.ArrayList<>();
        String sql = "SELECT si.*, m.medicine_name, m.hsn_code, m.schedule_type, m.packing, (m.cgst + m.sgst) as gst_rate, " +
                     "comp.company_name, " +
                     "mb.batch_number, DATE_FORMAT(mb.expiry_date, '%m/%y') as exp_date " +
                     "FROM sale_items si " +
                     "JOIN medicines m ON si.medicine_id = m.medicine_id " +
                     "JOIN medicine_batches mb ON si.batch_id = mb.batch_id " +
                     "LEFT JOIN medicine_companies comp ON m.company_id = comp.company_id " +
                     "WHERE si.sale_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, saleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.pharmacyerp.model.CartItem item = new com.pharmacyerp.model.CartItem();
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setHsnCode(rs.getString("hsn_code") != null ? rs.getString("hsn_code") : "");
                    item.setScheduleType(rs.getString("schedule_type") != null ? rs.getString("schedule_type") : "");
                    item.setPacking(rs.getString("packing") != null ? rs.getString("packing") : "");
                    item.setCompanyName(rs.getString("company_name") != null ? rs.getString("company_name") : "");
                    
                    item.setBatchId(rs.getInt("batch_id"));
                    item.setBatchNumber(rs.getString("batch_number") != null ? rs.getString("batch_number") : "");
                    item.setExpiryDateStr(rs.getString("exp_date") != null ? rs.getString("exp_date") : "");
                    
                    item.setGstRate(rs.getBigDecimal("gst_rate") != null ? rs.getBigDecimal("gst_rate") : java.math.BigDecimal.ZERO);
                    item.setMrp(rs.getBigDecimal("mrp"));
                    item.setSellingRate(rs.getBigDecimal("selling_rate"));
                    item.setDiscountPercentage(rs.getBigDecimal("discount_percentage"));
                    item.setQuantity(rs.getInt("quantity")); // This will trigger calculateTotals() using gstRate and sellingRate
                    
                    items.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching sale items", e);
        }
        return items;
    }
}
