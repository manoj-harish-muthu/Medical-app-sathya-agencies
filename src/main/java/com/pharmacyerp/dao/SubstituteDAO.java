package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Substitute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SubstituteDAO {
    private static final Logger logger = LoggerFactory.getLogger(SubstituteDAO.class);

    public List<Substitute> getAllSubstitutes() {
        List<Substitute> list = new ArrayList<>();
        String sql = "SELECT s.id, s.medicine_id, s.substitute_medicine_id, " +
                     "m1.medicine_name AS medicine_name, m2.medicine_name AS substitute_name " +
                     "FROM medicine_substitutes s " +
                     "JOIN medicines m1 ON s.medicine_id = m1.medicine_id " +
                     "JOIN medicines m2 ON s.substitute_medicine_id = m2.medicine_id " +
                     "ORDER BY m1.medicine_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Substitute sub = new Substitute();
                sub.setId(rs.getInt("id"));
                sub.setMedicineId(rs.getInt("medicine_id"));
                sub.setSubstituteMedicineId(rs.getInt("substitute_medicine_id"));
                sub.setMedicineName(rs.getString("medicine_name"));
                sub.setSubstituteName(rs.getString("substitute_name"));
                list.add(sub);
            }
        } catch (Exception e) {
            logger.error("Error fetching substitutes", e);
        }
        return list;
    }

    public boolean addSubstitute(Substitute sub) {
        String sql = "INSERT INTO medicine_substitutes (medicine_id, substitute_medicine_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sub.getMedicineId());
            stmt.setInt(2, sub.getSubstituteMedicineId());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error adding substitute", e);
            return false;
        }
    }

    public boolean deleteSubstitute(int id) {
        String sql = "DELETE FROM medicine_substitutes WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error deleting substitute", e);
            return false;
        }
    }
}
