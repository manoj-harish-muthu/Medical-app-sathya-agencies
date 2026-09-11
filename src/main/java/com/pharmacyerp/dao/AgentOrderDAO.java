package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.AgentOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class AgentOrderDAO {
    private static final Logger logger = LoggerFactory.getLogger(AgentOrderDAO.class);

    public void saveOrSyncOrder(AgentOrder order) {
        if (order == null) return;

        // 1. Sync or register customer in the customers table if phone is provided
        syncCustomerRecord(order);

        // 2. Insert or update agent_orders table
        String checkSql = "SELECT order_id FROM agent_orders WHERE customer_id = ? OR source_file = ? LIMIT 1";
        String insertSql = "INSERT INTO agent_orders (customer_id, customer_name, phone, delivery_address, " +
                "is_confirmed, is_checked_by_admin, admin_status, order_source, source_file, total_items, order_timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE agent_orders SET customer_name = ?, phone = ?, delivery_address = ?, " +
                "is_confirmed = ?, is_checked_by_admin = ?, admin_status = ?, total_items = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE order_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            Integer existingOrderId = null;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, order.getCustomerId());
                checkStmt.setString(2, order.getFilePath() != null ? order.getFilePath() : "");
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        existingOrderId = rs.getInt("order_id");
                        order.setId(existingOrderId);
                    }
                }
            }

            int itemsCount = order.getItems() != null ? order.getItems().size() : 0;

            if (existingOrderId != null) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, order.getCustomerName());
                    updateStmt.setString(2, order.getPhone());
                    updateStmt.setString(3, order.getAddress());
                    updateStmt.setBoolean(4, order.isConfirmed());
                    updateStmt.setBoolean(5, order.isCheckedByAdmin());
                    updateStmt.setString(6, order.getAdminStatus());
                    updateStmt.setInt(7, itemsCount);
                    updateStmt.setInt(8, existingOrderId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, order.getCustomerId());
                    insertStmt.setString(2, order.getCustomerName());
                    insertStmt.setString(3, order.getPhone());
                    insertStmt.setString(4, order.getAddress());
                    insertStmt.setBoolean(5, order.isConfirmed());
                    insertStmt.setBoolean(6, order.isCheckedByAdmin());
                    insertStmt.setString(7, order.getAdminStatus());
                    insertStmt.setString(8, order.getOrderSource());
                    insertStmt.setString(9, order.getFilePath() != null ? order.getFilePath() : "");
                    insertStmt.setInt(10, itemsCount);
                    insertStmt.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                    insertStmt.executeUpdate();

                    try (ResultSet gk = insertStmt.getGeneratedKeys()) {
                        if (gk.next()) {
                            existingOrderId = gk.getInt(1);
                            order.setId(existingOrderId);
                        }
                    }
                }
            }

            // Sync items
            if (existingOrderId != null && order.getItems() != null) {
                // Delete previous items and re-insert to keep them 100% accurate
                String deleteItemsSql = "DELETE FROM agent_order_items WHERE order_id = ?";
                try (PreparedStatement delStmt = conn.prepareStatement(deleteItemsSql)) {
                    delStmt.setInt(1, existingOrderId);
                    delStmt.executeUpdate();
                }

                String insertItemSql = "INSERT INTO agent_order_items (order_id, medicine_name, quantity, unit, volume_ml, pieces_per_unit) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql)) {
                    for (AgentOrder.OrderItem it : order.getItems()) {
                        itemStmt.setInt(1, existingOrderId);
                        itemStmt.setString(2, it.getMedicine());
                        itemStmt.setString(3, it.getQuantity());
                        itemStmt.setString(4, it.getUnit());
                        itemStmt.setString(5, it.getVolumeMl());
                        if (it.getPiecesPerUnit() != null) {
                            itemStmt.setInt(6, it.getPiecesPerUnit());
                        } else {
                            itemStmt.setNull(6, Types.INTEGER);
                        }
                        itemStmt.addBatch();
                    }
                    itemStmt.executeBatch();
                }
            }

        } catch (Exception e) {
            logger.error("Error saving/syncing AgentOrder to database: {}", e.getMessage(), e);
        }
    }

    public void updateOrderStatus(String filePath, String customerId, String adminStatus, boolean isCheckedByAdmin) {
        String sql = "UPDATE agent_orders SET admin_status = ?, is_checked_by_admin = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE source_file = ? OR customer_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, adminStatus);
            stmt.setBoolean(2, isCheckedByAdmin);
            stmt.setString(3, filePath != null ? filePath : "");
            stmt.setString(4, customerId != null ? customerId : "");
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.warn("Could not update agent_order status in DB: {}", e.getMessage());
        }
    }

    private void syncCustomerRecord(AgentOrder order) {
        if (order.getPhone() == null || order.getPhone().isBlank()) return;
        String phone = order.getPhone().trim();

        String checkCustomer = "SELECT customer_id FROM customers WHERE phone = ? LIMIT 1";
        String insertCustomer = "INSERT INTO customers (name, phone, address) VALUES (?, ?, ?)";
        String updateCustomer = "UPDATE customers SET name = ?, address = ? WHERE phone = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            boolean exists = false;
            try (PreparedStatement stmt = conn.prepareStatement(checkCustomer)) {
                stmt.setString(1, phone);
                try (ResultSet rs = stmt.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (!exists) {
                try (PreparedStatement ins = conn.prepareStatement(insertCustomer)) {
                    ins.setString(1, order.getCustomerName());
                    ins.setString(2, phone);
                    ins.setString(3, order.getAddress());
                    ins.executeUpdate();
                    logger.info("Auto-registered WhatsApp customer in ERP database: {} ({})", order.getCustomerName(), phone);
                }
            } else {
                try (PreparedStatement upd = conn.prepareStatement(updateCustomer)) {
                    upd.setString(1, order.getCustomerName());
                    upd.setString(2, order.getAddress());
                    upd.setString(3, phone);
                    upd.executeUpdate();
                }
            }
        } catch (Exception e) {
            logger.warn("Could not sync customer record into customers table: {}", e.getMessage());
        }
    }
}
