package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Brand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BrandDAO {
    private static final Logger logger = LoggerFactory.getLogger(BrandDAO.class);

    public List<Brand> getAllBrands() {
        List<Brand> list = new ArrayList<>();
        String sql = "SELECT * FROM medicine_brands ORDER BY brand_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Brand b = new Brand();
                b.setBrandId(rs.getInt("brand_id"));
                b.setBrandName(rs.getString("brand_name"));
                b.setDescription(rs.getString("description"));
                list.add(b);
            }
        } catch (Exception e) {
            logger.error("Error fetching brands", e);
        }
        return list;
    }

    public boolean addBrand(Brand b) {
        String sql = "INSERT INTO medicine_brands (brand_name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, b.getBrandName());
            stmt.setString(2, b.getDescription());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error adding brand", e);
            return false;
        }
    }

    public boolean updateBrand(Brand b) {
        String sql = "UPDATE medicine_brands SET brand_name=?, description=? WHERE brand_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, b.getBrandName());
            stmt.setString(2, b.getDescription());
            stmt.setInt(3, b.getBrandId());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating brand", e);
            return false;
        }
    }
}
