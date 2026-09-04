package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Sale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OutstandingDAO {
    private static final Logger logger = LoggerFactory.getLogger(OutstandingDAO.class);

    public List<Sale> getOutstandingBills() {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE balance_amount > 0 ORDER BY sale_date DESC";

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
                sale.setGrandTotal(rs.getBigDecimal("grand_total"));
                sale.setPaidAmount(rs.getBigDecimal("paid_amount"));
                sale.setBalanceAmount(rs.getBigDecimal("balance_amount"));
                sales.add(sale);
            }
        } catch (Exception e) {
            logger.error("Error fetching outstanding bills", e);
        }
        return sales;
    }

    public boolean addPayment(int saleId, String customerPhone, BigDecimal amount, String paymentMode, String notes) {
        String insertPaymentSql = "INSERT INTO customer_payments (sale_id, customer_phone, amount_paid, payment_mode, notes) VALUES (?, ?, ?, ?, ?)";
        String updateSaleSql = "UPDATE sales SET paid_amount = paid_amount + ?, balance_amount = balance_amount - ? WHERE sale_id = ?";
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement payStmt = conn.prepareStatement(insertPaymentSql);
                 PreparedStatement saleStmt = conn.prepareStatement(updateSaleSql)) {
                
                payStmt.setInt(1, saleId);
                payStmt.setString(2, customerPhone);
                payStmt.setBigDecimal(3, amount);
                payStmt.setString(4, paymentMode);
                payStmt.setString(5, notes);
                payStmt.executeUpdate();
                
                saleStmt.setBigDecimal(1, amount);
                saleStmt.setBigDecimal(2, amount);
                saleStmt.setInt(3, saleId);
                saleStmt.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (Exception e) {
            logger.error("Error adding payment", e);
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) {}
            }
        }
    }
}
