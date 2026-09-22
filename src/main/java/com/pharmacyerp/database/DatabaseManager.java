package com.pharmacyerp.database;

import com.pharmacyerp.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;
    private static boolean isPostgreSql = false;

    public static void initialize() {
        try {
            // Ensure data directory exists
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            int poolSize = ConfigManager.getInt("DB_POOL_SIZE", 10);
            HikariConfig config = new HikariConfig();
            config.setMaximumPoolSize(poolSize);

            // 1. Check for hosted Neon / Postgres URL first
            String neonUrl = ConfigManager.get("NEON_DB_URL");
            if (neonUrl == null || neonUrl.isBlank()) {
                neonUrl = ConfigManager.get("DATABASE_URL");
            }

            // 2. Check if NEON_DB_URL / DATABASE_URL or DB_URL indicates PostgreSQL
            String dbUrlOverride = ConfigManager.get("DB_URL");
            String activeUrl = (neonUrl != null && !neonUrl.isBlank()) ? neonUrl.trim() : (dbUrlOverride != null ? dbUrlOverride.trim() : null);

            if (activeUrl != null && (activeUrl.startsWith("postgres://") || activeUrl.startsWith("postgresql://") || activeUrl.startsWith("jdbc:postgresql:"))) {
                isPostgreSql = true;
                configurePostgres(config, activeUrl);
            } else {
                isPostgreSql = false;
                configureMySql(config, activeUrl);
            }

            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized: {}", config.getPoolName());

            runMigrations();

        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void configurePostgres(HikariConfig config, String rawUrl) {
        config.setDriverClassName("org.postgresql.Driver");
        config.setPoolName("NeonDBPool");

        if (rawUrl.startsWith("jdbc:postgresql:")) {
            config.setJdbcUrl(rawUrl);
            String user = ConfigManager.get("DB_USER");
            String pass = ConfigManager.get("DB_PASSWORD");
            if (user != null && !user.isBlank()) config.setUsername(user);
            if (pass != null && !pass.isBlank()) config.setPassword(pass);
            logger.info("Configured Neon/PostgreSQL connection via JDBC URL: {}", maskCredentials(rawUrl));
            return;
        }

        try {
            // Normalize postgres:// to postgresql:// for URI parsing
            String normalizedUri = rawUrl.replaceFirst("^postgres://", "postgresql://");
            URI uri = new URI(normalizedUri);

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }
            String dbName = (path != null && !path.isBlank()) ? path : "neondb";

            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                if (parts.length > 1) {
                    password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }

            // Neon requires sslmode=require
            String query = uri.getQuery();
            StringBuilder jdbcUrl = new StringBuilder();
            jdbcUrl.append("jdbc:postgresql://").append(host).append(":").append(port).append("/").append(dbName);
            if (query != null && !query.isBlank()) {
                jdbcUrl.append("?").append(query);
            } else {
                jdbcUrl.append("?sslmode=require");
            }

            config.setJdbcUrl(jdbcUrl.toString());
            if (username != null) {
                config.setUsername(username);
            } else {
                String envUser = ConfigManager.get("DB_USER");
                if (envUser != null && !envUser.isBlank()) config.setUsername(envUser);
            }

            if (password != null) {
                config.setPassword(password);
            } else {
                String envPass = ConfigManager.get("DB_PASSWORD");
                if (envPass != null && !envPass.isBlank()) config.setPassword(envPass);
            }

            logger.info("Configured Neon DB connection for host: {}, database: {}", host, dbName);

        } catch (Exception e) {
            logger.warn("Could not parse PostgreSQL URI, falling back to direct string: {}", e.getMessage());
            String jdbcUrl = rawUrl.startsWith("jdbc:") ? rawUrl : ("jdbc:" + rawUrl);
            config.setJdbcUrl(jdbcUrl);
        }
    }

    private static void configureMySql(HikariConfig config, String explicitJdbcUrl) {
        String host = ConfigManager.get("DB_HOST", "localhost");
        String port = ConfigManager.get("DB_PORT", "3306");
        String dbName = ConfigManager.get("DB_NAME", "pharmacy_erp");
        String username = ConfigManager.get("DB_USER", "root");
        String password = ConfigManager.get("DB_PASSWORD", "");

        // Automatically check and create database if it doesn't exist yet (local MySQL only)
        ensureDatabaseExists(host, port, dbName, username, password);

        String jdbcUrl = explicitJdbcUrl;
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
        config.setPoolName("MySQLPool");
        logger.info("Configured MySQL connection for host: {}, database: {}", host, dbName);
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
        String migrationLocation = isPostgreSql ? "classpath:db/migration/postgres" : "classpath:db/migration/mysql";
        logger.info("Running database migrations from {} (isPostgres: {})...", migrationLocation, isPostgreSql);
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(migrationLocation)
                    .baselineOnMigrate(true)
                    .load();

            flyway.repair();
            flyway.migrate();
            logger.info("Database migrations completed successfully.");
        } catch (Exception e) {
            logger.error("Database migration failed on {}: {}", (isPostgreSql ? "PostgreSQL/NeonDB" : "MySQL"), e.getMessage());
            throw e;
        }
    }

    public static boolean isPostgres() {
        return isPostgreSql;
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

    private static String maskCredentials(String url) {
        if (url == null) return "";
        return url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
    }
}
