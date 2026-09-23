package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.GenericSalt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GenericSaltDAO {
    private static final Logger logger = LoggerFactory.getLogger(GenericSaltDAO.class);

    public List<GenericSalt> getAllSalts() {
        List<GenericSalt> list = new ArrayList<>();
        String sql = "SELECT * FROM medicine_salts ORDER BY salt_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                GenericSalt salt = new GenericSalt();
                salt.setSaltId(rs.getInt("salt_id"));
                salt.setSaltName(rs.getString("salt_name"));
                salt.setDescription(rs.getString("description"));
                list.add(salt);
            }
        } catch (Exception e) {
            logger.error("Error fetching salts", e);
        }
        return list;
    }

    public boolean addSalt(GenericSalt salt) {
        String sql = "INSERT INTO medicine_salts (salt_name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, salt.getSaltName());
            stmt.setString(2, salt.getDescription());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error adding salt", e);
            return false;
        }
    }

    public boolean updateSalt(GenericSalt salt) {
        String sql = "UPDATE medicine_salts SET salt_name=?, description=? WHERE salt_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, salt.getSaltName());
            stmt.setString(2, salt.getDescription());
            stmt.setInt(3, salt.getSaltId());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating salt", e);
            return false;
        }
    }
}
