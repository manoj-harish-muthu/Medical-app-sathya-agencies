package com.pharmacyerp.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigManagerTest {

    @Test
    public void testGetDefaultProperties() {
        assertEquals("Sathya Agencies", ConfigManager.get("app.name"));
        assertEquals("localhost", ConfigManager.get("db.host"));
        assertEquals(10, ConfigManager.getInt("db.pool.size", 5));
    }

    @Test
    public void testKeyNormalization() {
        // Looking up UPPER_SNAKE or lower.dot should work interchangeably
        assertEquals(ConfigManager.get("db.host"), ConfigManager.get("DB_HOST"));
        assertEquals(ConfigManager.get("db.name"), ConfigManager.get("DB_NAME"));
    }

    @Test
    public void testFallbackValues() {
        assertEquals("fallback_val", ConfigManager.get("NON_EXISTING_KEY", "fallback_val"));
        assertEquals(42, ConfigManager.getInt("NON_EXISTING_INT", 42));
        assertTrue(ConfigManager.getBoolean("NON_EXISTING_BOOL", true));
        assertFalse(ConfigManager.getBoolean("NON_EXISTING_BOOL_2", false));
    }
}
