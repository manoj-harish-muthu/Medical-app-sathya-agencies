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
        if (query == null || query.trim().isEmpty()) return null;
        String trimmed = query.trim();

        String sql = "SELECT m.medicine_id, m.medicine_name, m.hsn_code, COALESCE(NULLIF(m.cgst + m.sgst, 0), m.gst_rate, 0) as gst_rate, m.schedule_type, m.packing, " +
                     "comp.company_name, " +
                     "mb.batch_id, mb.batch_number, mb.mrp, mb.selling_rate, mb.expiry_date " +
                     "FROM medicines m " +
                     "JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                     "LEFT JOIN medicine_companies comp ON m.company_id = comp.company_id " +
                     "WHERE mb.batch_number = ? OR m.medicine_name = ? OR m.medicine_name LIKE ? OR m.salt_name LIKE ? " +
                     "ORDER BY CASE WHEN m.medicine_name = ? THEN 1 ELSE 0 END DESC, " +
                     "CASE WHEN mb.current_quantity > 0 THEN 1 ELSE 0 END DESC, mb.expiry_date ASC LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, trimmed);
            stmt.setString(2, trimmed);
            stmt.setString(3, "%" + trimmed + "%");
            stmt.setString(4, "%" + trimmed + "%");
            stmt.setString(5, trimmed);
            
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
                    java.sql.Date expDate = rs.getDate("expiry_date");
                    item.setExpiryDateStr(expDate != null ? new java.text.SimpleDateFormat("MM/yy").format(expDate) : "");
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
        if (query == null || query.trim().isEmpty()) return items;
        String trimmed = query.trim();

        String sql = "SELECT m.medicine_id, m.medicine_name, m.hsn_code, COALESCE(NULLIF(m.cgst + m.sgst, 0), m.gst_rate, 0) as gst_rate, m.schedule_type, m.packing, " +
                     "comp.company_name, " +
                     "mb.batch_id, mb.batch_number, mb.mrp, mb.selling_rate, mb.expiry_date, mb.current_quantity " +
                     "FROM medicines m " +
                     "JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                     "LEFT JOIN medicine_companies comp ON m.company_id = comp.company_id " +
                     "WHERE mb.batch_number = ? OR m.medicine_name LIKE ? OR m.salt_name LIKE ? OR comp.company_name LIKE ? " +
                     "ORDER BY CASE WHEN m.medicine_name LIKE ? THEN 1 ELSE 0 END DESC, " +
                     "CASE WHEN mb.current_quantity > 0 THEN 1 ELSE 0 END DESC, m.medicine_name ASC, mb.expiry_date ASC LIMIT 15";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, trimmed);
            stmt.setString(2, "%" + trimmed + "%");
            stmt.setString(3, "%" + trimmed + "%");
            stmt.setString(4, "%" + trimmed + "%");
            stmt.setString(5, trimmed + "%");
            
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
                    java.sql.Date expDate = rs.getDate("expiry_date");
                    item.setExpiryDateStr(expDate != null ? new java.text.SimpleDateFormat("MM/yy").format(expDate) : "");
                    item.setAvailableStock(rs.getInt("current_quantity"));
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

    public String getCustomerNameByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null;
        String sql = "SELECT name FROM customers WHERE phone = ? UNION SELECT customer_name FROM sales WHERE customer_phone = ? AND customer_name != '' LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone.trim());
            stmt.setString(2, phone.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            logger.error("Error looking up customer phone", e);
        }
        return null;
    }

    public static class LastProductSale {
        public String date;
        public java.math.BigDecimal rate;
    }

    public LastProductSale getLastSaleOfProduct(String customerName, int medicineId) {
        if (customerName == null || customerName.trim().isEmpty()) return null;
        String sql = "SELECT TO_CHAR(s.sale_date, 'DD/MM/YYYY') as date, si.selling_rate " +
                     "FROM sales s " +
                     "JOIN sale_items si ON s.sale_id = si.sale_id " +
                     "WHERE s.customer_name = ? AND si.medicine_id = ? " +
                     "ORDER BY s.sale_date DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerName.trim());
            stmt.setInt(2, medicineId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LastProductSale lps = new LastProductSale();
                    lps.date = rs.getString("date");
                    lps.rate = rs.getBigDecimal("selling_rate");
                    return lps;
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching last sale of product", e);
        }
        return null;
    }

    public String getCustomerPhoneByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT phone FROM customers WHERE name = ? UNION SELECT customer_phone FROM sales WHERE customer_name = ? AND customer_phone != '' LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            stmt.setString(2, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            logger.error("Error looking up customer name", e);
        }
        return null;
    }

    public static class CustomerHistory {
        public String invoiceNo;
        public String date;
        public java.math.BigDecimal amount;
    }

    public java.util.List<CustomerHistory> getCustomerHistory(String name) {
        java.util.List<CustomerHistory> history = new java.util.ArrayList<>();
        if (name == null || name.trim().isEmpty()) return history;
        String sql = "SELECT invoice_no, TO_CHAR(sale_date, 'DD/MM/YYYY') as date, grand_total " +
                     "FROM sales WHERE customer_name = ? ORDER BY sale_date DESC LIMIT 5";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CustomerHistory h = new CustomerHistory();
                    h.invoiceNo = rs.getString("invoice_no");
                    h.date = rs.getString("date");
                    h.amount = rs.getBigDecimal("grand_total");
                    history.add(h);
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching customer history", e);
        }
        return history;
    }

    public java.util.List<String> getAllCustomerNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        String sql = "SELECT name FROM customers WHERE name IS NOT NULL AND name != '' UNION SELECT name FROM patients WHERE name IS NOT NULL AND name != '' ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.error("Error loading customer names", e);
        }
        return names;
    }

    public boolean saveSale(String invoiceNo, String customerName, String customerPhone, String doctorName, String paymentMode, java.util.List<CartItem> cartItems, java.math.BigDecimal paidAmount, java.math.BigDecimal balanceAmount) {
        return saveSale(invoiceNo, customerName, customerPhone, doctorName, paymentMode, cartItems, paidAmount, balanceAmount, "COMPLETED");
    }

    public boolean saveSale(String invoiceNo, String customerName, String customerPhone, String doctorName, String paymentMode, java.util.List<CartItem> cartItems, java.math.BigDecimal paidAmount, java.math.BigDecimal balanceAmount, String status) {
        if (cartItems == null || cartItems.isEmpty()) {
            logger.warn("Cannot save sale: cartItems is empty");
            return false;
        }

        String safeCustomer = customerName != null ? customerName.trim() : "";
        String safePhone = customerPhone != null ? customerPhone.trim() : "";
        if (safePhone.length() > 20) {
            safePhone = safePhone.substring(0, 20);
        }
        String safeDoctor = doctorName != null ? doctorName.trim() : "";
        String safePayment = (paymentMode != null && !paymentMode.trim().isEmpty()) ? paymentMode.trim() : "Cash";
        String safeStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : "COMPLETED";

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
                java.math.BigDecimal itemRate = item.getSellingRate() != null ? item.getSellingRate() : java.math.BigDecimal.ZERO;
                subtotal = subtotal.add(itemRate.multiply(new java.math.BigDecimal(item.getQuantity())));
                discount = discount.add(item.getDiscountAmount() != null ? item.getDiscountAmount() : java.math.BigDecimal.ZERO);
                cgst = cgst.add(item.getCgstAmount() != null ? item.getCgstAmount() : java.math.BigDecimal.ZERO);
                sgst = sgst.add(item.getSgstAmount() != null ? item.getSgstAmount() : java.math.BigDecimal.ZERO);
                total = total.add(item.getNetAmount() != null ? item.getNetAmount() : java.math.BigDecimal.ZERO);
            }

            // Rounding logic for Grand Total matching the invoice
            java.math.BigDecimal roundOff = total.setScale(0, java.math.RoundingMode.HALF_UP).subtract(total);
            java.math.BigDecimal grandTotal = total.add(roundOff);

            java.math.BigDecimal finalPaid = paidAmount != null ? paidAmount : grandTotal;
            java.math.BigDecimal finalBalance = balanceAmount != null ? balanceAmount : grandTotal.subtract(finalPaid).max(java.math.BigDecimal.ZERO);

            int saleId = 0;
            try (PreparedStatement saleStmt = conn.prepareStatement(insertSaleSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                saleStmt.setString(1, invoiceNo);
                saleStmt.setString(2, safeCustomer);
                saleStmt.setString(3, safePhone);
                saleStmt.setString(4, safeDoctor);
                saleStmt.setString(5, safePayment);
                saleStmt.setBigDecimal(6, subtotal);
                saleStmt.setBigDecimal(7, discount);
                saleStmt.setBigDecimal(8, cgst);
                saleStmt.setBigDecimal(9, sgst);
                saleStmt.setBigDecimal(10, grandTotal);
                saleStmt.setBigDecimal(11, finalPaid);
                saleStmt.setBigDecimal(12, finalBalance);
                saleStmt.setString(13, safeStatus);
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

            if (!"QUOTATION".equals(safeStatus) && finalPaid.compareTo(java.math.BigDecimal.ZERO) > 0) {
                try (PreparedStatement payStmt = conn.prepareStatement(insertPaymentSql)) {
                    payStmt.setInt(1, saleId);
                    payStmt.setString(2, safePhone);
                    payStmt.setBigDecimal(3, finalPaid);
                    payStmt.setString(4, safePayment);
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

                    // Update Inventory (Skip for Quotations)
                    if (!"QUOTATION".equals(safeStatus)) {
                        updateStmt.setInt(1, item.getQuantity());
                        updateStmt.setInt(2, item.getBatchId());
                        updateStmt.addBatch();
                    }
                }

                itemStmt.executeBatch();
                if (!"QUOTATION".equals(safeStatus)) {
                    updateStmt.executeBatch();
                }
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
