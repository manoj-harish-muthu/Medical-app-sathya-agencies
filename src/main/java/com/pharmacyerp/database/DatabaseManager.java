package com.pharmacyerp.database;

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

            HikariConfig config = new HikariConfig();
            // MySQL Configuration
            // Ensure you have created the database first: CREATE DATABASE pharmacy_erp;
            config.setJdbcUrl("jdbc:mysql://localhost:3306/pharmacy_erp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
            config.setUsername("root");
            config.setPassword("manojmagesh"); // Change this if your MySQL password is empty ("") or different
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection Pool settings
            config.setMaximumPoolSize(10);
            config.setPoolName("MySQLPool");

            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized.");

            runMigrations();

        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void runMigrations() {
        logger.info("Running database migrations...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();

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
