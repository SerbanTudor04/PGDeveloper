package ro.fintechpro.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSourceManager {

    private HikariDataSource dataSource;

    /**
     * Connects to a specific PostgreSQL database.
     * @param host     e.g., "localhost"
     * @param port     e.g., 5432
     * @param database e.g., "postgres"
     * @param user     e.g., "postgres"
     * @param password e.g., "secret"
     */
    public void connect(String host, int port, String database, String user, String password, boolean useSsl) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }

        HikariConfig config = new HikariConfig();

        // Dynamic JDBC URL
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(user);
        config.setPassword(password);
        config.addDataSourceProperty("loggerLevel", "TRACE");
        config.addDataSourceProperty("loggerFile", "pg_debug.log");
        // SSL Configuration
        if (useSsl) {
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", "require");
            // This factory allows self-signed certificates (common in dev/test environments)
            config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
        }

        config.setConnectionTimeout(5000);
        config.setMaximumPoolSize(5);

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Returns a valid connection from the pool.
     * The caller is responsible for closing this connection (try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Not connected to any database. Call connect() first.");
        }
        return dataSource.getConnection();
    }

    /**
     * Simple test method to check if the connection is valid.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.execute("SELECT 1");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}