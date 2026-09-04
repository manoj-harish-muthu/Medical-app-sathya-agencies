package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private static final Logger logger = LoggerFactory.getLogger(CategoryDAO.class);

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM medicine_categories ORDER BY category_name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Category c = new Category();
                c.setCategoryId(rs.getInt("category_id"));
                c.setCategoryName(rs.getString("category_name"));
                c.setDescription(rs.getString("description"));
                list.add(c);
            }
        } catch (Exception e) {
            logger.error("Error fetching categories", e);
        }
        return list;
    }

    public boolean addCategory(Category c) {
        String sql = "INSERT INTO medicine_categories (category_name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, c.getCategoryName());
            stmt.setString(2, c.getDescription());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error adding category", e);
            return false;
        }
    }

    public boolean updateCategory(Category c) {
        String sql = "UPDATE medicine_categories SET category_name=?, description=? WHERE category_id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, c.getCategoryName());
            stmt.setString(2, c.getDescription());
            stmt.setInt(3, c.getCategoryId());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating category", e);
            return false;
        }
    }
}
