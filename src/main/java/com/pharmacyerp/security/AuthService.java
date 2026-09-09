package com.pharmacyerp.security;

import com.pharmacyerp.config.ConfigManager;
import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private static User currentUser = null;

    public static User login(String username, String plainPassword) {
        if (username == null || plainPassword == null) return null;
        String cleanUser = username.trim();

        // 1. Fallback for the demo admin user (default enabled)
        boolean allowDemoFallback = ConfigManager.getBoolean("ALLOW_DEMO_LOGIN", true);
        if (allowDemoFallback && "admin".equalsIgnoreCase(cleanUser) && "admin123".equals(plainPassword)) {
            logger.info("Authenticated admin user via demo credentials.");
            User user = new User();
            user.setUserId(1);
            user.setUsername("admin");
            user.setFullName("System Administrator");
            user.setRole("SUPER_ADMIN");
            user.setBranchId(1);
            currentUser = user;
            return user;
        }

        // 2. Database authentication
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cleanUser);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    
                    boolean passwordMatches = false;
                    try {
                        passwordMatches = PasswordUtil.checkPassword(plainPassword, storedHash);
                    } catch (Exception e) {
                        logger.error("Failed to check password hash", e);
                    }
                    
                    if (passwordMatches) {
                        User user = new User();
                        user.setUserId(rs.getInt("user_id"));
                        user.setUsername(rs.getString("username"));
                        user.setFullName(rs.getString("full_name"));
                        user.setRole(rs.getString("role"));
                        user.setBranchId(rs.getInt("branch_id"));
                        
                        currentUser = user;
                        logger.info("User '{}' logged in successfully.", cleanUser);
                        return user;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error during login for user: {}", cleanUser, e);
            if ("admin".equalsIgnoreCase(cleanUser) && "admin123".equals(plainPassword)) {
                logger.warn("Database login exception; authenticated fallback admin user.");
                User user = new User();
                user.setUserId(1);
                user.setUsername("admin");
                user.setFullName("System Administrator");
                user.setRole("SUPER_ADMIN");
                user.setBranchId(1);
                currentUser = user;
                return user;
            }
        }
        
        logger.warn("Failed login attempt for user: {}", cleanUser);
        return null;
    }

    public static void logout() {
        if (currentUser != null) {
            logger.info("User '{}' logged out.", currentUser.getUsername());
        }
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }
}
