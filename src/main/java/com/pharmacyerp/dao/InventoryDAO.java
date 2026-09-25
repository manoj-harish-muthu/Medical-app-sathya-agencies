package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.InventoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;
import java.time.LocalDate;

public class InventoryDAO {
    private static final Logger logger = LoggerFactory.getLogger(InventoryDAO.class);

    public List<InventoryItem> getAllInventory() {
        return fetchInventory("SELECT m.medicine_name, m.minimum_stock, mb.batch_number, mb.expiry_date, mb.current_quantity, mb.mrp " +
                "FROM medicines m JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                "ORDER BY m.medicine_name ASC");
    }

    public List<InventoryItem> getExpiredStock() {
        return fetchInventory("SELECT m.medicine_name, m.minimum_stock, mb.batch_number, mb.expiry_date, mb.current_quantity, mb.mrp " +
                "FROM medicines m JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                "WHERE mb.expiry_date < CURRENT_DATE " +
                "ORDER BY mb.expiry_date ASC");
    }

    public List<InventoryItem> getNearExpiryStock() {
        Date cutoff = Date.valueOf(LocalDate.now().plusDays(90));
        return fetchInventory("SELECT m.medicine_name, m.minimum_stock, mb.batch_number, mb.expiry_date, mb.current_quantity, mb.mrp " +
                "FROM medicines m JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                "WHERE mb.expiry_date >= CURRENT_DATE AND mb.expiry_date <= ? " +
                "ORDER BY mb.expiry_date ASC", cutoff);
    }

    public List<InventoryItem> getZeroOrLowStock() {
        return fetchInventory("SELECT m.medicine_name, m.minimum_stock, mb.batch_number, mb.expiry_date, mb.current_quantity, mb.mrp " +
                "FROM medicines m JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                "WHERE mb.current_quantity <= m.minimum_stock " +
                "ORDER BY mb.current_quantity ASC");
    }

    private List<InventoryItem> fetchInventory(String sql, Object... params) {
        List<InventoryItem> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setMedicineName(rs.getString("medicine_name"));
                item.setBatchNumber(rs.getString("batch_number"));
                
                Date expiry = rs.getDate("expiry_date");
                item.setExpiryDate(expiry);
                
                int stock = rs.getInt("current_quantity");
                item.setCurrentStock(stock);
                
                int minStock = rs.getInt("minimum_stock");
                item.setMinStock(minStock);
                
                item.setMrp(rs.getBigDecimal("mrp"));

                // Determine Status
                LocalDate expDate = expiry.toLocalDate();
                if (expDate.isBefore(today)) {
                    item.setStatus("Expired");
                } else if (expDate.isBefore(today.plusDays(90))) {
                    item.setStatus("Near Expiry");
                } else if (stock == 0) {
                    item.setStatus("Zero Stock");
                } else if (stock <= minStock) {
                    item.setStatus("Low Stock");
                } else {
                    item.setStatus("Normal");
                }

                list.add(item);
            }
            }
        } catch (Exception e) {
            logger.error("Error fetching inventory", e);
        }
        return list;
    }

    public List<InventoryItem> searchInventory(String query) {
        String sql = "SELECT m.medicine_name, m.minimum_stock, mb.batch_number, mb.expiry_date, mb.current_quantity, mb.mrp " +
                "FROM medicines m JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                "WHERE mb.batch_number LIKE ? OR m.medicine_name LIKE ? " +
                "ORDER BY m.medicine_name ASC LIMIT 50";
        
        List<InventoryItem> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, query + "%");
            stmt.setString(2, query + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InventoryItem item = new InventoryItem();
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setBatchNumber(rs.getString("batch_number"));
                    item.setExpiryDate(rs.getDate("expiry_date"));
                    item.setCurrentStock(rs.getInt("current_quantity"));
                    item.setMinStock(rs.getInt("minimum_stock"));
                    item.setMrp(rs.getBigDecimal("mrp"));
                    list.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Error searching inventory", e);
        }
        return list;
    }

    public boolean updateStock(String batchNumber, int newQuantity) {
        String sql = "UPDATE medicine_batches SET current_quantity = ? WHERE batch_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setInt(1, newQuantity);
            stmt.setString(2, batchNumber);
            return stmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            logger.error("Error updating stock", e);
            return false;
        }
    }

    public List<com.pharmacyerp.model.ExpiryReportRow> getExpiringBatches(int monthsUntilExpiry) {
        List<com.pharmacyerp.model.ExpiryReportRow> list = new ArrayList<>();
        
        String sql = "SELECT m.medicine_name, mb.batch_number, mb.expiry_date, mb.current_quantity, comp.company_name " +
                     "FROM medicine_batches mb " +
                     "JOIN medicines m ON mb.medicine_id = m.medicine_id " +
                     "LEFT JOIN medicine_companies comp ON m.company_id = comp.company_id " +
                     "WHERE mb.current_quantity > 0 " +
                     "AND mb.expiry_date <= CURRENT_DATE + INTERVAL '" + monthsUntilExpiry + " months' " +
                     "ORDER BY mb.expiry_date ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next()) {
                java.sql.Date expDate = rs.getDate("expiry_date");
                list.add(new com.pharmacyerp.model.ExpiryReportRow(
                        rs.getString("medicine_name"),
                        rs.getString("batch_number"),
                        expDate != null ? expDate.toLocalDate() : null,
                        rs.getInt("current_quantity"),
                        rs.getString("company_name")
                ));
            }
        } catch (Exception e) {
            logger.error("Error fetching expiring batches", e);
        }
        return list;
    }
}
