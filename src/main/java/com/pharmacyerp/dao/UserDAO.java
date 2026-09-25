package com.pharmacyerp.dao;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.User;
import com.pharmacyerp.security.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(rs.getString("role"));
                user.setBranchId(rs.getInt("branch_id"));
                user.setPermissions(rs.getString("permissions"));
                users.add(user);
            }
        } catch (Exception e) {
            logger.error("Error fetching all users", e);
        }
        return users;
    }

    public boolean createUser(String username, String plainPassword, String fullName, String role, String permissions) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, branch_id, permissions) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String hash = PasswordUtil.hashPassword(plainPassword);

            stmt.setString(1, username);
            stmt.setString(2, hash);
            stmt.setString(3, fullName);
            stmt.setString(4, role);
            stmt.setInt(5, 1); // Default branch_id
            stmt.setString(6, permissions);

            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return false;
        }
    }
}
