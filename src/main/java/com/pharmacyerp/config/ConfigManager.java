package com.pharmacyerp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Unified configuration manager supporting:
 * 1. OS Environment variables (highest priority)
 * 2. JVM System properties (-Dkey=value)
 * 3. Local .env file (in working directory or data/.env)
 * 4. External config.properties file
 * 5. Bundled application.properties classpath defaults (lowest priority)
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Map<String, String> configMap = new HashMap<>();

    static {
        loadConfiguration();
    }

    public static synchronized void loadConfiguration() {
        configMap.clear();

        // 1. Load bundled default application.properties from classpath
        loadFromClasspath("application.properties");

        // 2. Load from external config.properties (if present)
        loadFromPropertiesFile("config.properties");
        loadFromPropertiesFile("data/config.properties");

        // 3. Load from .env file (if present)
        loadFromEnvFile(".env");
        loadFromEnvFile("data/.env");

        // Check custom path via system property if provided: -Denv.file=...
        String customEnv = System.getProperty("env.file");
        if (customEnv != null && !customEnv.isBlank()) {
            loadFromEnvFile(customEnv);
        }
    }

    private static void loadFromClasspath(String resourceName) {
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                Properties props = new Properties();
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String key : props.stringPropertyNames()) {
                    putNormalized(key, props.getProperty(key));
                }
                logger.debug("Loaded default properties from classpath: {}", resourceName);
            }
        } catch (Exception e) {
            logger.warn("Could not load default properties from classpath: {}", resourceName, e);
        }
    }

    private static void loadFromPropertiesFile(String filePath) {
        Path path = Paths.get(filePath);
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                Properties props = new Properties();
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String key : props.stringPropertyNames()) {
                    putNormalized(key, props.getProperty(key));
                }
                logger.info("Loaded configuration from properties file: {}", filePath);
            } catch (Exception e) {
                logger.warn("Failed to read properties file: {}", filePath, e);
            }
        }
    }

    private static void loadFromEnvFile(String filePath) {
        Path path = Paths.get(filePath);
        if (Files.isRegularFile(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        // Strip quotes if present
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            if (value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            }
                        }
                        putNormalized(key, value);
                    }
                }
                logger.info("Loaded configuration from .env file: {}", filePath);
            } catch (Exception e) {
                logger.warn("Failed to read .env file: {}", filePath, e);
            }
        }
    }

    private static void putNormalized(String key, String value) {
        if (key == null) return;
        configMap.put(key, value);
        // Store both UPPER_SNAKE and lower.dot versions for easy retrieval
        String upperSnake = key.replace('.', '_').toUpperCase();
        String lowerDot = key.replace('_', '.').toLowerCase();
        configMap.put(upperSnake, value);
        configMap.put(lowerDot, value);
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String defaultValue) {
        if (key == null) return defaultValue;

        // 1. Highest priority: System Environment Variable
        String envVal = System.getenv(key);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        String upperEnv = key.replace('.', '_').toUpperCase();
        envVal = System.getenv(upperEnv);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }

        // 2. JVM System Property
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        sysProp = System.getProperty(upperEnv);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }

        // 3. Loaded files (env / properties)
        if (configMap.containsKey(key)) {
            return configMap.get(key);
        }
        if (configMap.containsKey(upperEnv)) {
            return configMap.get(upperEnv);
        }
        String lowerDot = key.replace('_', '.').toLowerCase();
        if (configMap.containsKey(lowerDot)) {
            return configMap.get(lowerDot);
        }

        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer configuration for key '{}': '{}', using default {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(val.trim()) || "1".equals(val.trim());
    }
}
