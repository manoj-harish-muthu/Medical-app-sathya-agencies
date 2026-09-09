package com.pharmacyerp.database;

import com.pharmacyerp.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;

    public static void initialize() {
        try {
            // Ensure data directory exists
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            String host = ConfigManager.get("DB_HOST", "localhost");
            String port = ConfigManager.get("DB_PORT", "3306");
            String dbName = ConfigManager.get("DB_NAME", "pharmacy_erp");
            String username = ConfigManager.get("DB_USER", "root");
            String password = ConfigManager.get("DB_PASSWORD", "");
            int poolSize = ConfigManager.getInt("DB_POOL_SIZE", 10);

            // Automatically check and create database if it doesn't exist yet
            ensureDatabaseExists(host, port, dbName, username, password);

            HikariConfig config = new HikariConfig();
            
            // Resolve JDBC URL from ConfigManager (DB_URL or constructed from host/port/name)
            String jdbcUrl = ConfigManager.get("DB_URL");
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                jdbcUrl = String.format(
                    "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    host, port, dbName
                );
            }

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection Pool settings
            config.setMaximumPoolSize(poolSize);
            config.setPoolName("MySQLPool");

            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized.");

            runMigrations();

        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void ensureDatabaseExists(String host, String port, String dbName, String username, String password) {
        String serverUrl = String.format("jdbc:mysql://%s:%s/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", host, port);
        try (Connection conn = java.sql.DriverManager.getConnection(serverUrl, username, password);
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            logger.info("Database '{}' verified/provisioned on MySQL server.", dbName);
        } catch (Exception e) {
            logger.warn("Could not auto-verify database '{}' on MySQL server: {}", dbName, e.getMessage());
        }
    }

    private static void runMigrations() {
        logger.info("Running database migrations...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.repair();
        flyway.migrate();
        logger.info("Database migrations completed.");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager is not initialized.");
        }
        return dataSource.getConnection();
    }
    
    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }
}
