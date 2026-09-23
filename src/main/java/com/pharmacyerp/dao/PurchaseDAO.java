package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Purchase;
import com.pharmacyerp.model.PurchaseItem;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class PurchaseDAO {

    public static int savePurchase(Purchase purchase, List<PurchaseItem> items) throws SQLException {
        String insertPurchaseSql = "INSERT INTO purchases (invoice_no, purchase_date, supplier_id, supplier_name, payment_mode, subtotal, total_tax, total_discount, grand_total, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO purchase_items (purchase_id, medicine_id, batch_number, manufacturing_date, expiry_date, quantity, free_quantity, purchase_rate, mrp, selling_rate, discount_percentage, tax_percentage, tax_amount, net_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String checkBatchSql = "SELECT current_quantity FROM medicine_batches WHERE medicine_id = ? AND batch_number = ?";
        String updateBatchSql = "UPDATE medicine_batches SET current_quantity = current_quantity + ? WHERE medicine_id = ? AND batch_number = ?";
        String insertBatchSql = "INSERT INTO medicine_batches (medicine_id, batch_number, manufacturing_date, expiry_date, purchase_rate, mrp, selling_rate, current_quantity) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Transaction start

            int purchaseId = 0;
            try (PreparedStatement stmt = conn.prepareStatement(insertPurchaseSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, purchase.getInvoiceNo());
                stmt.setTimestamp(2, Timestamp.valueOf(purchase.getPurchaseDate()));
                stmt.setInt(3, purchase.getSupplierId());
                stmt.setString(4, purchase.getSupplierName());
                stmt.setString(5, purchase.getPaymentMode());
                stmt.setDouble(6, purchase.getSubtotal());
                stmt.setDouble(7, purchase.getTotalTax());
                stmt.setDouble(8, purchase.getTotalDiscount());
                stmt.setDouble(9, purchase.getGrandTotal());
                stmt.setString(10, purchase.getStatus());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating purchase failed, no rows affected.");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        purchaseId = generatedKeys.getInt(1);
                        purchase.setPurchaseId(purchaseId);
                    } else {
                        throw new SQLException("Creating purchase failed, no ID obtained.");
                    }
                }
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql);
                 PreparedStatement checkBatchStmt = conn.prepareStatement(checkBatchSql);
                 PreparedStatement updateBatchStmt = conn.prepareStatement(updateBatchSql);
                 PreparedStatement insertBatchStmt = conn.prepareStatement(insertBatchSql)) {
                 
                for (PurchaseItem item : items) {
                    itemStmt.setInt(1, purchaseId);
                    itemStmt.setInt(2, item.getMedicineId());
                    itemStmt.setString(3, item.getBatchNumber());
                    itemStmt.setDate(4, item.getManufacturingDate() != null ? Date.valueOf(item.getManufacturingDate()) : null);
                    itemStmt.setDate(5, Date.valueOf(item.getExpiryDate()));
                    itemStmt.setInt(6, item.getQuantity());
                    itemStmt.setInt(7, item.getFreeQuantity());
                    itemStmt.setDouble(8, item.getPurchaseRate());
                    itemStmt.setDouble(9, item.getMrp());
                    itemStmt.setDouble(10, item.getSellingRate());
                    itemStmt.setDouble(11, item.getDiscountPercentage());
                    itemStmt.setDouble(12, item.getTaxPercentage());
                    itemStmt.setDouble(13, item.getTaxAmount());
                    itemStmt.setDouble(14, item.getNetAmount());
                    itemStmt.executeUpdate();

                    // Handle inventory update
                    int totalQty = item.getQuantity() + item.getFreeQuantity();
                    checkBatchStmt.setInt(1, item.getMedicineId());
                    checkBatchStmt.setString(2, item.getBatchNumber());
                    
                    boolean batchExists = false;
                    try (ResultSet rs = checkBatchStmt.executeQuery()) {
                        if (rs.next()) {
                            batchExists = true;
                        }
                    }

                    if (batchExists) {
                        updateBatchStmt.setInt(1, totalQty);
                        updateBatchStmt.setInt(2, item.getMedicineId());
                        updateBatchStmt.setString(3, item.getBatchNumber());
                        updateBatchStmt.executeUpdate();
                    } else {
                        insertBatchStmt.setInt(1, item.getMedicineId());
                        insertBatchStmt.setString(2, item.getBatchNumber());
                        insertBatchStmt.setDate(3, item.getManufacturingDate() != null ? Date.valueOf(item.getManufacturingDate()) : null);
                        insertBatchStmt.setDate(4, Date.valueOf(item.getExpiryDate()));
                        insertBatchStmt.setDouble(5, item.getPurchaseRate());
                        insertBatchStmt.setDouble(6, item.getMrp());
                        insertBatchStmt.setDouble(7, item.getSellingRate());
                        insertBatchStmt.setInt(8, totalQty);
                        insertBatchStmt.executeUpdate();
                    }
                }
            }

            conn.commit();
            return purchaseId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public static double getLastPurchaseTotalBySupplier(int supplierId) {
        String sql = "SELECT grand_total FROM purchases WHERE supplier_id = ? ORDER BY purchase_date DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supplierId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("grand_total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
