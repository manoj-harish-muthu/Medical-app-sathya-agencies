package com.pharmacyerp.repository;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Medicine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MedicineRepository {

    private static final String BASE_SELECT = 
        "SELECT m.*, c.category_name, comp.company_name, b.brand_name, s.schedule_name, " +
        "(SELECT COALESCE(SUM(current_quantity), 0) FROM medicine_batches WHERE medicine_id = m.medicine_id AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE)) as total_stock, " +
        "(SELECT mrp FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as current_mrp, " +
        "(SELECT batch_number FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as batch_number, " +
        "(SELECT expiry_date FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as expiry_date, " +
        "(SELECT purchase_rate FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as purchase_rate, " +
        "(SELECT selling_rate FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as selling_rate, " +
        "(SELECT rate_a FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as rate_a, " +
        "(SELECT rate_b FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as rate_b, " +
        "(SELECT rate_c FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as rate_c, " +
        "(SELECT cost_per_pcs FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as cost_per_pcs, " +
        "(SELECT conv_str FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as conv_str, " +
        "(SELECT conv_cas FROM medicine_batches WHERE medicine_id = m.medicine_id ORDER BY (current_quantity > 0) DESC, expiry_date ASC LIMIT 1) as conv_cas " +
        "FROM medicines m " +
        "LEFT JOIN medicine_categories c ON m.category_id = c.category_id " +
        "LEFT JOIN medicine_companies comp ON m.company_id = comp.company_id " +
        "LEFT JOIN medicine_brands b ON m.brand_id = b.brand_id " +
        "LEFT JOIN medicine_schedules s ON m.schedule_id = s.schedule_id " +
        "WHERE m.active = 1 ";

    public List<Medicine> getAllActiveMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY m.medicine_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                medicines.add(mapResultSetToMedicine(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return medicines;
    }

    public List<Medicine> searchMedicines(String query) {
        List<Medicine> medicines = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return getAllActiveMedicines();
        }
        String trimmed = query.trim();
        String sql = BASE_SELECT + "AND (m.medicine_name LIKE ? OR m.salt_name LIKE ? OR comp.company_name LIKE ? OR c.category_name LIKE ? OR m.hsn_code LIKE ?) " +
                     "ORDER BY (m.medicine_name LIKE ?) DESC, m.medicine_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String param = "%" + trimmed + "%";
            stmt.setString(1, param);
            stmt.setString(2, param);
            stmt.setString(3, param);
            stmt.setString(4, param);
            stmt.setString(5, param);
            stmt.setString(6, trimmed + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    medicines.add(mapResultSetToMedicine(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return medicines;
    }

    private Medicine mapResultSetToMedicine(ResultSet rs) throws java.sql.SQLException {
        Medicine med = new Medicine();
        med.setMedicineId(rs.getInt("medicine_id"));
        med.setMedicineName(rs.getString("medicine_name"));
        med.setSaltName(rs.getString("salt_name") != null ? rs.getString("salt_name") : "");
        med.setCompanyId(rs.getInt("company_id"));
        med.setCategoryId(rs.getInt("category_id"));
        med.setHsnCode(rs.getString("hsn_code") != null ? rs.getString("hsn_code") : "");
        med.setGstRate(rs.getDouble("gst_rate"));
        med.setPrescriptionRequired(rs.getBoolean("prescription_required"));
        med.setScheduleType(rs.getString("schedule_type") != null ? rs.getString("schedule_type") : "");
        med.setPackSize(rs.getString("pack_size") != null ? rs.getString("pack_size") : "");
        med.setPacking(rs.getString("packing") != null ? rs.getString("packing") : "");
        med.setBatchNumber(rs.getString("batch_number") != null ? rs.getString("batch_number") : "");
        
        med.setCategoryName(rs.getString("category_name") != null ? rs.getString("category_name") : "");
        med.setCompanyName(rs.getString("company_name") != null ? rs.getString("company_name") : "");
        med.setBrandId(rs.getInt("brand_id"));
        med.setScheduleId(rs.getInt("schedule_id"));
        med.setBrandName(rs.getString("brand_name") != null ? rs.getString("brand_name") : "");
        med.setScheduleName(rs.getString("schedule_name") != null ? rs.getString("schedule_name") : "");
        med.setTotalStock(rs.getInt("total_stock"));
        med.setCurrentMrp(rs.getDouble("current_mrp"));

        // Extended Marg ERP fields
        med.setUnit1st(rs.getString("unit_1st") != null ? rs.getString("unit_1st") : "");
        med.setUnit2nd(rs.getString("unit_2nd") != null ? rs.getString("unit_2nd") : "");
        med.setDecimalAllowed(rs.getString("decimal_allowed") != null ? rs.getString("decimal_allowed") : "No");
        med.setColorType(rs.getString("color_type") != null ? rs.getString("color_type") : "NORMAL");
        med.setItemType(rs.getString("item_type") != null ? rs.getString("item_type") : "1 NORMAL");
        med.setCgst(rs.getDouble("cgst"));
        med.setSgst(rs.getDouble("sgst"));
        med.setIgst(rs.getDouble("igst"));
        med.setLocalTaxType(rs.getString("local_tax_type") != null ? rs.getString("local_tax_type") : "Taxable");
        med.setCentralTaxType(rs.getString("central_tax_type") != null ? rs.getString("central_tax_type") : "Taxable");
        med.setNegativeAllowed(rs.getString("negative_allowed") != null ? rs.getString("negative_allowed") : "No");

        // Batch pricing fields
        java.sql.Date expDate = rs.getDate("expiry_date");
        if (expDate != null) {
            med.setExpiryDate(expDate.toLocalDate());
        }
        med.setMrp(rs.getDouble("current_mrp"));
        med.setPurchaseRate(rs.getDouble("purchase_rate"));
        med.setSellingRate(rs.getDouble("selling_rate"));
        med.setRateA(rs.getDouble("rate_a"));
        med.setRateB(rs.getDouble("rate_b"));
        med.setRateC(rs.getDouble("rate_c"));
        med.setCostPerPcs(rs.getDouble("cost_per_pcs"));
        med.setConvStr(rs.getInt("conv_str"));
        med.setConvCas(rs.getInt("conv_cas"));

        return med;
    }

    public boolean saveMedicineWithBatch(Medicine med) {
        String insertMedicineSql = "INSERT INTO medicines (medicine_name, salt_name, company_id, category_id, brand_id, schedule_id, active, packing, unit_1st, unit_2nd, decimal_allowed, color_type, item_type, cgst, sgst, igst, local_tax_type, central_tax_type, negative_allowed) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertBatchSql = "INSERT INTO medicine_batches (medicine_id, batch_number, expiry_date, purchase_rate, mrp, selling_rate, current_quantity, rate_a, rate_b, rate_c, cost_per_pcs, conv_str, conv_cas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            int medicineId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(insertMedicineSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, med.getMedicineName());
                stmt.setString(2, med.getSaltName());
                
                if (med.getCompanyId() > 0) stmt.setInt(3, med.getCompanyId()); else stmt.setNull(3, java.sql.Types.INTEGER);
                if (med.getCategoryId() > 0) stmt.setInt(4, med.getCategoryId()); else stmt.setNull(4, java.sql.Types.INTEGER);
                if (med.getBrandId() > 0) stmt.setInt(5, med.getBrandId()); else stmt.setNull(5, java.sql.Types.INTEGER);
                if (med.getScheduleId() > 0) stmt.setInt(6, med.getScheduleId()); else stmt.setNull(6, java.sql.Types.INTEGER);
                
                stmt.setString(7, med.getPacking());
                stmt.setString(8, med.getUnit1st());
                stmt.setString(9, med.getUnit2nd());
                stmt.setString(10, med.getDecimalAllowed());
                stmt.setString(11, med.getColorType());
                stmt.setString(12, med.getItemType());
                stmt.setDouble(13, med.getCgst());
                stmt.setDouble(14, med.getSgst());
                stmt.setDouble(15, med.getIgst());
                stmt.setString(16, med.getLocalTaxType());
                stmt.setString(17, med.getCentralTaxType());
                stmt.setString(18, med.getNegativeAllowed());
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
        String updateMedicineSql = "UPDATE medicines SET medicine_name=?, salt_name=?, company_id=?, category_id=?, brand_id=?, schedule_id=?, packing=?, unit_1st=?, unit_2nd=?, decimal_allowed=?, color_type=?, item_type=?, cgst=?, sgst=?, igst=?, local_tax_type=?, central_tax_type=?, negative_allowed=? WHERE medicine_id=?";
        String updateBatchSql = "UPDATE medicine_batches SET batch_number=?, expiry_date=?, purchase_rate=?, mrp=?, selling_rate=?, current_quantity=?, rate_a=?, rate_b=?, rate_c=?, cost_per_pcs=?, conv_str=?, conv_cas=? WHERE medicine_id=?";
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(updateMedicineSql)) {
                stmt.setString(1, med.getMedicineName());
                stmt.setString(2, med.getSaltName());
                
                if (med.getCompanyId() > 0) stmt.setInt(3, med.getCompanyId()); else stmt.setNull(3, java.sql.Types.INTEGER);
                if (med.getCategoryId() > 0) stmt.setInt(4, med.getCategoryId()); else stmt.setNull(4, java.sql.Types.INTEGER);
                if (med.getBrandId() > 0) stmt.setInt(5, med.getBrandId()); else stmt.setNull(5, java.sql.Types.INTEGER);
                if (med.getScheduleId() > 0) stmt.setInt(6, med.getScheduleId()); else stmt.setNull(6, java.sql.Types.INTEGER);
                
                stmt.setString(7, med.getPacking());
                stmt.setString(8, med.getUnit1st());
                stmt.setString(9, med.getUnit2nd());
                stmt.setString(10, med.getDecimalAllowed());
                stmt.setString(11, med.getColorType());
                stmt.setString(12, med.getItemType());
                stmt.setDouble(13, med.getCgst());
                stmt.setDouble(14, med.getSgst());
                stmt.setDouble(15, med.getIgst());
                stmt.setString(16, med.getLocalTaxType());
                stmt.setString(17, med.getCentralTaxType());
                stmt.setString(18, med.getNegativeAllowed());
                stmt.setInt(19, med.getMedicineId());
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
