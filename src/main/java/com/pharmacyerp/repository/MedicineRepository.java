package com.pharmacyerp.repository;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Medicine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MedicineRepository {

    public List<Medicine> getAllActiveMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT m.*, c.category_name, comp.company_name, " +
                     "(SELECT SUM(current_quantity) FROM medicine_batches WHERE medicine_id = m.medicine_id AND expiry_date > date('now')) as total_stock, " +
                     "(SELECT mrp FROM medicine_batches WHERE medicine_id = m.medicine_id AND current_quantity > 0 ORDER BY expiry_date ASC LIMIT 1) as current_mrp, " +
                     "(SELECT batch_number FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY expiry_date ASC LIMIT 1) as batch_number " +
                     "FROM medicines m " +
                     "LEFT JOIN medicine_categories c ON m.category_id = c.category_id " +
                     "LEFT JOIN medicine_companies comp ON m.company_id = comp.company_id " +
                     "WHERE m.active = 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Medicine med = new Medicine();
                med.setMedicineId(rs.getInt("medicine_id"));
                med.setMedicineName(rs.getString("medicine_name"));
                med.setSaltName(rs.getString("salt_name"));
                med.setCompanyId(rs.getInt("company_id"));
                med.setCategoryId(rs.getInt("category_id"));
                med.setHsnCode(rs.getString("hsn_code"));
                med.setGstRate(rs.getDouble("gst_rate"));
                med.setPrescriptionRequired(rs.getBoolean("prescription_required"));
                med.setScheduleType(rs.getString("schedule_type"));
                med.setPackSize(rs.getString("pack_size"));
                med.setPacking(rs.getString("packing"));
                med.setBatchNumber(rs.getString("batch_number"));
                
                med.setCategoryName(rs.getString("category_name"));
                med.setCompanyName(rs.getString("company_name"));
                med.setTotalStock(rs.getInt("total_stock"));
                med.setCurrentMrp(rs.getDouble("current_mrp"));
                
                medicines.add(med);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return medicines;
    }

    public boolean saveMedicineWithBatch(Medicine med) {
        String insertMedicineSql = "INSERT INTO medicines (medicine_name, salt_name, active, packing, unit_1st, unit_2nd, decimal_allowed, color_type, item_type, cgst, sgst, igst, local_tax_type, central_tax_type, negative_allowed) VALUES (?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertBatchSql = "INSERT INTO medicine_batches (medicine_id, batch_number, expiry_date, purchase_rate, mrp, selling_rate, current_quantity, rate_a, rate_b, rate_c, cost_per_pcs, conv_str, conv_cas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            int medicineId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(insertMedicineSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, med.getMedicineName());
                stmt.setString(2, med.getSaltName());
                stmt.setString(3, med.getPacking());
                stmt.setString(4, med.getUnit1st());
                stmt.setString(5, med.getUnit2nd());
                stmt.setString(6, med.getDecimalAllowed());
                stmt.setString(7, med.getColorType());
                stmt.setString(8, med.getItemType());
                stmt.setDouble(9, med.getCgst());
                stmt.setDouble(10, med.getSgst());
                stmt.setDouble(11, med.getIgst());
                stmt.setString(12, med.getLocalTaxType());
                stmt.setString(13, med.getCentralTaxType());
                stmt.setString(14, med.getNegativeAllowed());
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        medicineId = rs.getInt(1);
                    }
                }
            }
            
            if (medicineId != -1) {
                try (PreparedStatement stmt = conn.prepareStatement(insertBatchSql)) {
                    stmt.setInt(1, medicineId);
                    stmt.setString(2, med.getBatchNumber());
                    stmt.setDate(3, java.sql.Date.valueOf(med.getExpiryDate()));
                    stmt.setDouble(4, med.getPurchaseRate());
                    stmt.setDouble(5, med.getMrp());
                    stmt.setDouble(6, med.getSellingRate());
                    stmt.setInt(7, med.getQuantity());
                    stmt.setDouble(8, med.getRateA());
                    stmt.setDouble(9, med.getRateB());
                    stmt.setDouble(10, med.getRateC());
                    stmt.setDouble(11, med.getCostPerPcs());
                    stmt.setInt(12, med.getConvStr());
                    stmt.setInt(13, med.getConvCas());
                    stmt.executeUpdate();
                }
            } else {
                conn.rollback();
                return false;
            }
            
            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean updateMedicineWithBatch(Medicine med) {
        String updateMedicineSql = "UPDATE medicines SET medicine_name=?, salt_name=?, packing=?, unit_1st=?, unit_2nd=?, decimal_allowed=?, color_type=?, item_type=?, cgst=?, sgst=?, igst=?, local_tax_type=?, central_tax_type=?, negative_allowed=? WHERE medicine_id=?";
        String updateBatchSql = "UPDATE medicine_batches SET batch_number=?, expiry_date=?, purchase_rate=?, mrp=?, selling_rate=?, current_quantity=?, rate_a=?, rate_b=?, rate_c=?, cost_per_pcs=?, conv_str=?, conv_cas=? WHERE medicine_id=?";
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(updateMedicineSql)) {
                stmt.setString(1, med.getMedicineName());
                stmt.setString(2, med.getSaltName());
                stmt.setString(3, med.getPacking());
                stmt.setString(4, med.getUnit1st());
                stmt.setString(5, med.getUnit2nd());
                stmt.setString(6, med.getDecimalAllowed());
                stmt.setString(7, med.getColorType());
                stmt.setString(8, med.getItemType());
                stmt.setDouble(9, med.getCgst());
                stmt.setDouble(10, med.getSgst());
                stmt.setDouble(11, med.getIgst());
                stmt.setString(12, med.getLocalTaxType());
                stmt.setString(13, med.getCentralTaxType());
                stmt.setString(14, med.getNegativeAllowed());
                stmt.setInt(15, med.getMedicineId());
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(updateBatchSql)) {
                stmt.setString(1, med.getBatchNumber());
                stmt.setDate(2, java.sql.Date.valueOf(med.getExpiryDate()));
                stmt.setDouble(3, med.getPurchaseRate());
                stmt.setDouble(4, med.getMrp());
                stmt.setDouble(5, med.getSellingRate());
                stmt.setInt(6, med.getQuantity());
                stmt.setDouble(7, med.getRateA());
                stmt.setDouble(8, med.getRateB());
                stmt.setDouble(9, med.getRateC());
                stmt.setDouble(10, med.getCostPerPcs());
                stmt.setInt(11, med.getConvStr());
                stmt.setInt(12, med.getConvCas());
                stmt.setInt(13, med.getMedicineId());
                stmt.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    public boolean deleteMedicine(int medicineId) {
        String sql = "UPDATE medicines SET active = 0 WHERE medicine_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, medicineId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
