package com.pharmacyerp.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Hashes a plaintext password using BCrypt.
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    /**
     * Checks a plaintext password against a stored hash.
     */
    public static boolean checkPassword(String plainPassword, String storedHash) {
        if (storedHash == null || storedHash.length() < 10) {
            return false;
        }
        String normalizedHash = storedHash;
        if (storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            normalizedHash = "$2a$" + storedHash.substring(4);
        }
        if (!normalizedHash.startsWith("$2a$")) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, normalizedHash);
    }
}
