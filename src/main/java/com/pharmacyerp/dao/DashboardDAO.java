package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigDecimal;

public class DashboardDAO {
    private static final Logger logger = LoggerFactory.getLogger(DashboardDAO.class);

    public int getTodaysBillsCount() {
        String sql = "SELECT COUNT(*) FROM sales WHERE DATE(sale_date) = CURRENT_DATE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("Error fetching today's bills", e);
        }
        return 0;
    }

    public BigDecimal getTodaysSalesTotal() {
        String sql = "SELECT SUM(grand_total) FROM sales WHERE DATE(sale_date) = CURRENT_DATE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal(1);
                return total != null ? total : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            logger.error("Error fetching today's sales", e);
        }
        return BigDecimal.ZERO;
    }

    public int getLowStockCount() {
        String sql = "SELECT COUNT(DISTINCT m.medicine_id) FROM medicines m " +
                     "JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                     "GROUP BY m.medicine_id, m.reorder_level " +
                     "HAVING SUM(mb.current_quantity) <= m.reorder_level";
        
        int count = 0;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                count++;
            }
        } catch (Exception e) {
            logger.error("Error fetching low stock count", e);
        }
        return count;
    }

    public int getExpiringSoonCount() {
        String sql = "SELECT COUNT(*) FROM medicine_batches WHERE expiry_date <= ? AND current_quantity > 0";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(90)));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching expiring soon count", e);
        }
        return 0;
    }

    public java.util.List<String> getAlertMessages() {
        java.util.List<String> alerts = new java.util.ArrayList<>();
        
        String lowStockSql = "SELECT m.medicine_name, SUM(mb.current_quantity) as qty, m.reorder_level " +
                             "FROM medicines m " +
                             "JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                             "GROUP BY m.medicine_id, m.medicine_name, m.reorder_level " +
                             "HAVING SUM(mb.current_quantity) <= m.reorder_level";
                             
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(lowStockSql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                alerts.add("Low Stock: " + rs.getString("medicine_name") + " (Only " + rs.getInt("qty") + " left)");
            }
        } catch (Exception e) {
            logger.error("Error fetching low stock alerts", e);
        }

        String expiringSql = "SELECT m.medicine_name, mb.batch_number, mb.expiry_date " +
                             "FROM medicine_batches mb " +
                             "JOIN medicines m ON mb.medicine_id = m.medicine_id " +
                             "WHERE mb.expiry_date <= ? AND mb.current_quantity > 0 " +
                             "ORDER BY mb.expiry_date ASC";
                             
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(expiringSql)) {
            stmt.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(90)));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alerts.add("Expiring: " + rs.getString("medicine_name") + " (Batch " + rs.getString("batch_number") + " on " + rs.getDate("expiry_date") + ")");
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching expiring soon alerts", e);
        }
        
        return alerts;
    }

    public java.util.Map<String, BigDecimal> getSalesTrend(int days) {
        java.util.Map<String, BigDecimal> trend = new java.util.LinkedHashMap<>();
        String sql = "SELECT DATE(sale_date) as sdate, SUM(grand_total) as stotal FROM sales " +
                     "WHERE sale_date >= ? " +
                     "GROUP BY DATE(sale_date) ORDER BY sdate ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            java.time.LocalDate cutoff = java.time.LocalDate.now().minusDays(days - 1);
            stmt.setDate(1, java.sql.Date.valueOf(cutoff));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String dateStr = rs.getString("sdate");
                    BigDecimal total = rs.getBigDecimal("stotal");
                    trend.put(dateStr, total != null ? total : BigDecimal.ZERO);
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching sales trend", e);
        }
        return trend;
    }
}
