package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Composition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CompositionDAO {
    private static final Logger logger = LoggerFactory.getLogger(CompositionDAO.class);

    public List<Composition> getAllCompositions() {
        List<Composition> list = new ArrayList<>();
        String sql = "SELECT c.id, c.medicine_id, c.salt_id, c.strength, " +
                     "m.medicine_name AS medicine_name, s.salt_name AS salt_name " +
                     "FROM medicine_compositions c " +
                     "JOIN medicines m ON c.medicine_id = m.medicine_id " +
                     "JOIN medicine_salts s ON c.salt_id = s.salt_id " +
                     "ORDER BY m.medicine_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Composition comp = new Composition();
                comp.setId(rs.getInt("id"));
                comp.setMedicineId(rs.getInt("medicine_id"));
                comp.setSaltId(rs.getInt("salt_id"));
                comp.setMedicineName(rs.getString("medicine_name"));
                comp.setSaltName(rs.getString("salt_name"));
                comp.setStrength(rs.getString("strength"));
                list.add(comp);
            }
        } catch (Exception e) {
            logger.error("Error fetching compositions", e);
        }
        return list;
    }

    public boolean addComposition(Composition comp) {
        String sql = "INSERT INTO medicine_compositions (medicine_id, salt_id, strength) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, comp.getMedicineId());
            stmt.setInt(2, comp.getSaltId());
            stmt.setString(3, comp.getStrength());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error adding composition", e);
            return false;
        }
    }

    public boolean deleteComposition(int id) {
        String sql = "DELETE FROM medicine_compositions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error deleting composition", e);
            return false;
        }
    }
}
