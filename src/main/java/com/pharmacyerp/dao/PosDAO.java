package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.CartItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PosDAO {
    private static final Logger logger = LoggerFactory.getLogger(PosDAO.class);

    public CartItem getItemByBarcodeOrName(String query) {
        String sql = "SELECT m.medicine_id, m.medicine_name, m.hsn_code, (m.cgst + m.sgst) as gst_rate, m.schedule_type, m.packing, " +
                     "comp.company_name, " +
                     "mb.batch_id, mb.batch_number, mb.mrp, mb.selling_rate, DATE_FORMAT(mb.expiry_date, '%m/%y') as exp_date " +
                     "FROM medicines m " +
                     "JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                     "LEFT JOIN medicine_companies comp ON m.company_id = comp.company_id " +
                     "WHERE mb.batch_number = ? OR m.medicine_name LIKE ? " +
                     "ORDER BY mb.expiry_date ASC LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, query);
            stmt.setString(2, query + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CartItem item = new CartItem();
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setHsnCode(rs.getString("hsn_code") != null ? rs.getString("hsn_code") : "");
                    
                    java.math.BigDecimal gstRate = rs.getBigDecimal("gst_rate");
                    item.setGstRate(gstRate != null ? gstRate : java.math.BigDecimal.ZERO);
                    
                    item.setScheduleType(rs.getString("schedule_type") != null ? rs.getString("schedule_type") : "");
                    item.setPacking(rs.getString("packing") != null ? rs.getString("packing") : "");
                    item.setCompanyName(rs.getString("company_name") != null ? rs.getString("company_name") : "");
                    
                    item.setBatchId(rs.getInt("batch_id"));
                    item.setBatchNumber(rs.getString("batch_number") != null ? rs.getString("batch_number") : "");
                    item.setExpiryDateStr(rs.getString("exp_date") != null ? rs.getString("exp_date") : "");
                    item.setMrp(rs.getBigDecimal("mrp") != null ? rs.getBigDecimal("mrp") : java.math.BigDecimal.ZERO);
                    item.setSellingRate(rs.getBigDecimal("selling_rate") != null ? rs.getBigDecimal("selling_rate") : java.math.BigDecimal.ZERO);
                    
                    item.calculateTotals();
                    return item;
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching medicine for POS", e);
        }
        return null;
    }

    public java.util.List<CartItem> searchMedicines(String query) {
        java.util.List<CartItem> items = new java.util.ArrayList<>();
        String sql = "SELECT m.medicine_id, m.medicine_name, m.hsn_code, (m.cgst + m.sgst) as gst_rate, m.schedule_type, m.packing, " +
                     "comp.company_name, " +
                     "mb.batch_id, mb.batch_number, mb.mrp, mb.selling_rate, DATE_FORMAT(mb.expiry_date, '%m/%y') as exp_date " +
                     "FROM medicines m " +
                     "JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                     "LEFT JOIN medicine_companies comp ON m.company_id = comp.company_id " +
                     "WHERE mb.batch_number = ? OR m.medicine_name LIKE ? " +
                     "ORDER BY m.medicine_name ASC, mb.expiry_date ASC LIMIT 10";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, query);
            stmt.setString(2, query + "%"); // prefix search
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CartItem item = new CartItem();
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setHsnCode(rs.getString("hsn_code") != null ? rs.getString("hsn_code") : "");
                    
                    java.math.BigDecimal gstRate = rs.getBigDecimal("gst_rate");
                    item.setGstRate(gstRate != null ? gstRate : java.math.BigDecimal.ZERO);
                    
                    item.setScheduleType(rs.getString("schedule_type") != null ? rs.getString("schedule_type") : "");
                    item.setPacking(rs.getString("packing") != null ? rs.getString("packing") : "");
                    item.setCompanyName(rs.getString("company_name") != null ? rs.getString("company_name") : "");
                    
                    item.setBatchId(rs.getInt("batch_id"));
                    item.setBatchNumber(rs.getString("batch_number") != null ? rs.getString("batch_number") : "");
                    item.setExpiryDateStr(rs.getString("exp_date") != null ? rs.getString("exp_date") : "");
                    item.setMrp(rs.getBigDecimal("mrp") != null ? rs.getBigDecimal("mrp") : java.math.BigDecimal.ZERO);
                    item.setSellingRate(rs.getBigDecimal("selling_rate") != null ? rs.getBigDecimal("selling_rate") : java.math.BigDecimal.ZERO);
                    
                    item.calculateTotals();
                    items.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Error searching medicines for POS", e);
        }
        return items;
    }

    public boolean saveSale(String invoiceNo, String customerName, String customerPhone, String doctorName, String paymentMode, java.util.List<CartItem> cartItems, java.math.BigDecimal paidAmount, java.math.BigDecimal balanceAmount) {
        String insertSaleSql = "INSERT INTO sales (invoice_no, customer_name, customer_phone, doctor_name, payment_mode, subtotal, total_discount, total_cgst, total_sgst, grand_total, paid_amount, balance_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO sale_items (sale_id, medicine_id, batch_id, quantity, mrp, selling_rate, discount_percentage, cgst_amount, sgst_amount, net_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateBatchSql = "UPDATE medicine_batches SET current_quantity = current_quantity - ? WHERE batch_id = ?";
        String insertPaymentSql = "INSERT INTO customer_payments (sale_id, customer_phone, amount_paid, payment_mode) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
            java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
            java.math.BigDecimal cgst = java.math.BigDecimal.ZERO;
            java.math.BigDecimal sgst = java.math.BigDecimal.ZERO;
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;

            for (CartItem item : cartItems) {
                subtotal = subtotal.add(item.getSellingRate().multiply(new java.math.BigDecimal(item.getQuantity())));
                discount = discount.add(item.getDiscountAmount());
                cgst = cgst.add(item.getCgstAmount());
                sgst = sgst.add(item.getSgstAmount());
                total = total.add(item.getNetAmount());
            }

            // Rounding logic for Grand Total matching the invoice
            java.math.BigDecimal roundOff = total.setScale(0, java.math.RoundingMode.HALF_UP).subtract(total);
            java.math.BigDecimal grandTotal = total.add(roundOff);

            int saleId = 0;
            try (PreparedStatement saleStmt = conn.prepareStatement(insertSaleSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                saleStmt.setString(1, invoiceNo);
                saleStmt.setString(2, customerName);
                saleStmt.setString(3, customerPhone);
                saleStmt.setString(4, doctorName);
                saleStmt.setString(5, paymentMode);
                saleStmt.setBigDecimal(6, subtotal);
                saleStmt.setBigDecimal(7, discount);
                saleStmt.setBigDecimal(8, cgst);
                saleStmt.setBigDecimal(9, sgst);
                saleStmt.setBigDecimal(10, grandTotal);
                saleStmt.setBigDecimal(11, paidAmount);
                saleStmt.setBigDecimal(12, balanceAmount);
                saleStmt.setString(13, "COMPLETED");
                saleStmt.executeUpdate();

                try (ResultSet rs = saleStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                    }
                }
            }

            if (saleId == 0) {
                conn.rollback();
                return false;
            }

            if (paidAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                try (PreparedStatement payStmt = conn.prepareStatement(insertPaymentSql)) {
                    payStmt.setInt(1, saleId);
                    payStmt.setString(2, customerPhone);
                    payStmt.setBigDecimal(3, paidAmount);
                    payStmt.setString(4, paymentMode);
                    payStmt.executeUpdate();
                }
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateBatchSql)) {

                for (CartItem item : cartItems) {
                    // Insert Item
                    itemStmt.setInt(1, saleId);
                    itemStmt.setInt(2, item.getMedicineId());
                    itemStmt.setInt(3, item.getBatchId());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setBigDecimal(5, item.getMrp());
                    itemStmt.setBigDecimal(6, item.getSellingRate());
                    itemStmt.setBigDecimal(7, item.getDiscountPercentage());
                    itemStmt.setBigDecimal(8, item.getCgstAmount());
                    itemStmt.setBigDecimal(9, item.getSgstAmount());
                    itemStmt.setBigDecimal(10, item.getNetAmount());
                    itemStmt.addBatch();

                    // Update Inventory
                    updateStmt.setInt(1, item.getQuantity());
                    updateStmt.setInt(2, item.getBatchId());
                    updateStmt.addBatch();
                }

                itemStmt.executeBatch();
                updateStmt.executeBatch();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            logger.error("Error saving sale", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                    logger.error("Error rolling back", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }
}
