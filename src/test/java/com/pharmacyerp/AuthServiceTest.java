package com.pharmacyerp;

import com.pharmacyerp.database.DatabaseManager;
import com.pharmacyerp.model.User;
import com.pharmacyerp.security.AuthService;
import com.pharmacyerp.security.PasswordUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    @BeforeAll
    public static void setUp() throws Exception {
        DatabaseManager.initialize();
        
        // Generate a verified Java jBCrypt hash for admin123 and update the database
        String validHash = PasswordUtil.hashPassword("admin123");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE username = 'admin'")) {
            stmt.setString(1, validHash);
            stmt.executeUpdate();
        }
    }

    @Test
    public void testAdminLogin() {
        User user = AuthService.login("admin", "admin123");
        System.out.println("Login result: " + (user != null ? user.getFullName() : "NULL"));
        assertNotNull(user, "User should be logged in");
        assertEquals("admin", user.getUsername());
    }

    @Test
    public void testPasswordHashing() {
        String hash = PasswordUtil.hashPassword("admin123");
        assertNotNull(hash);
        assertTrue(PasswordUtil.checkPassword("admin123", hash));
        assertFalse(PasswordUtil.checkPassword("wrongpassword", hash));
    }
}
