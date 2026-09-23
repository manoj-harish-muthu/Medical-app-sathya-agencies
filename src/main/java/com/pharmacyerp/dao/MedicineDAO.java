package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Medicine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {
    
    public static List<Medicine> getAllMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT medicine_id, medicine_name FROM medicines ORDER BY medicine_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Medicine m = new Medicine();
                m.setMedicineId(rs.getInt("medicine_id"));
                m.setMedicineName(rs.getString("medicine_name"));
                medicines.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return medicines;
    }

    public static List<Medicine> searchMedicines(String query) {
        List<Medicine> medicines = new ArrayList<>();
        String sql = "SELECT m.medicine_id, m.medicine_name, m.salt_name, m.hsn_code, c.company_name " +
                     "FROM medicines m " +
                     "LEFT JOIN medicine_companies c ON m.company_id = c.company_id " +
                     "WHERE m.medicine_name LIKE ? OR m.salt_name LIKE ? LIMIT 50";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            String param = "%" + query + "%";
            stmt.setString(1, param);
            stmt.setString(2, param);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Medicine m = new Medicine();
                    m.setMedicineId(rs.getInt("medicine_id"));
                    m.setMedicineName(rs.getString("medicine_name"));
                    m.setSaltName(rs.getString("salt_name"));
                    m.setHsnCode(rs.getString("hsn_code"));
                    m.setCompanyName(rs.getString("company_name"));
                    medicines.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return medicines;
    }

    public static boolean saveErpMedicine(
            String status, String type, String hide,
            String productName, String packing, String unit1, String unit2, String decimal,
            String colorType, String itemType, String company, String salt, String hsn,
            String localTax, String centralTax, double sgst, double igst, double cgst,
            double mrp, double pRate, double cost, double rateA, double rateB, double rateC,
            int convStri, int convCas, String negative) {
        
        String insertMedicineSql = "INSERT INTO medicines (" +
                "medicine_name, packing, unit_1st, unit_2nd, decimal_allowed, " +
                "color_type, item_type, salt_name, hsn_code, " +
                "local_tax_type, central_tax_type, sgst, cgst, igst, negative_allowed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
        String insertBatchSql = "INSERT INTO medicine_batches (" +
                "medicine_id, batch_number, expiry_date, mrp, purchase_rate, selling_rate, " +
                "rate_a, rate_b, rate_c, cost_per_pcs, conv_str, conv_cas) " +
                "VALUES (?, 'DEFAULT', '2099-12-31', ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            // Insert core medicine details
            int medicineId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(insertMedicineSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, productName);
                stmt.setString(2, packing);
                stmt.setString(3, unit1);
                stmt.setString(4, unit2);
                stmt.setString(5, decimal);
                stmt.setString(6, colorType);
                stmt.setString(7, itemType);
                stmt.setString(8, salt); // Treating SALT as generic_name
                stmt.setString(9, hsn);
                stmt.setString(10, localTax);
                stmt.setString(11, centralTax);
                stmt.setDouble(12, sgst);
                stmt.setDouble(13, cgst);
                stmt.setDouble(14, igst);
                stmt.setString(15, negative);
                
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        medicineId = rs.getInt(1);
                    }
                }
            }
            
            // Insert default batch for pricing
            if (medicineId != -1) {
                try (PreparedStatement stmt = conn.prepareStatement(insertBatchSql)) {
                    stmt.setInt(1, medicineId);
                    stmt.setDouble(2, mrp);
                    stmt.setDouble(3, pRate);
                    stmt.setDouble(4, rateA); // selling_rate is rateA essentially
                    stmt.setDouble(5, rateA);
                    stmt.setDouble(6, rateB);
                    stmt.setDouble(7, rateC);
                    stmt.setDouble(8, cost);
                    stmt.setInt(9, convStri);
                    stmt.setInt(10, convCas);
                    stmt.executeUpdate();
                }
            }
            
            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    public static Medicine getMedicineById(int id) {
        String sql = "SELECT m.*, c.company_name, " +
                     "mb.batch_number, mb.expiry_date, mb.quantity, mb.purchase_rate, mb.mrp, mb.selling_rate, " +
                     "mb.rate_a, mb.rate_b, mb.rate_c, mb.cost_per_pcs, mb.conv_str, mb.conv_cas " +
                     "FROM medicines m " +
                     "LEFT JOIN medicine_companies c ON m.company_id = c.company_id " +
                     "LEFT JOIN medicine_batches mb ON m.medicine_id = mb.medicine_id " +
                     "WHERE m.medicine_id = ? " +
                     "ORDER BY mb.batch_id DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Medicine m = new Medicine();
                    m.setMedicineId(rs.getInt("medicine_id"));
                    m.setMedicineName(rs.getString("medicine_name"));
                    m.setSaltName(rs.getString("salt_name"));
                    m.setHsnCode(rs.getString("hsn_code"));
                    m.setCompanyName(rs.getString("company_name"));
                    m.setPacking(rs.getString("packing"));
                    m.setUnit1st(rs.getString("unit_1st"));
                    m.setUnit2nd(rs.getString("unit_2nd"));
                    m.setDecimalAllowed(rs.getString("decimal_allowed"));
                    m.setColorType(rs.getString("color_type"));
                    m.setItemType(rs.getString("item_type"));
                    m.setLocalTaxType(rs.getString("local_tax_type"));
                    m.setCentralTaxType(rs.getString("central_tax_type"));
                    
                    java.math.BigDecimal sgst = rs.getBigDecimal("sgst");
                    java.math.BigDecimal cgst = rs.getBigDecimal("cgst");
                    java.math.BigDecimal igst = rs.getBigDecimal("igst");
                    m.setSgst(sgst != null ? sgst.doubleValue() : 0.0);
                    m.setCgst(cgst != null ? cgst.doubleValue() : 0.0);
                    m.setIgst(igst != null ? igst.doubleValue() : 0.0);
                    
                    m.setNegativeAllowed(rs.getString("negative_allowed"));
                    
                    // Batch fields
                    m.setBatchNumber(rs.getString("batch_number"));
                    java.sql.Date expDate = rs.getDate("expiry_date");
                    if (expDate != null) m.setExpiryDate(expDate.toLocalDate());
                    
                    java.math.BigDecimal purchaseRate = rs.getBigDecimal("purchase_rate");
                    java.math.BigDecimal mrp = rs.getBigDecimal("mrp");
                    java.math.BigDecimal sellingRate = rs.getBigDecimal("selling_rate");
                    java.math.BigDecimal rateA = rs.getBigDecimal("rate_a");
                    java.math.BigDecimal rateB = rs.getBigDecimal("rate_b");
                    java.math.BigDecimal rateC = rs.getBigDecimal("rate_c");
                    java.math.BigDecimal costPerPcs = rs.getBigDecimal("cost_per_pcs");
                    
                    m.setPurchaseRate(purchaseRate != null ? purchaseRate.doubleValue() : 0.0);
                    m.setMrp(mrp != null ? mrp.doubleValue() : 0.0);
                    m.setSellingRate(sellingRate != null ? sellingRate.doubleValue() : 0.0);
                    m.setRateA(rateA != null ? rateA.doubleValue() : 0.0);
                    m.setRateB(rateB != null ? rateB.doubleValue() : 0.0);
                    m.setRateC(rateC != null ? rateC.doubleValue() : 0.0);
                    m.setCostPerPcs(costPerPcs != null ? costPerPcs.doubleValue() : 0.0);
                    
                    m.setConvStr(rs.getInt("conv_str"));
                    m.setConvCas(rs.getInt("conv_cas"));
                    
                    return m;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean deleteMedicine(int id) {
        String sql = "DELETE FROM medicines WHERE medicine_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
