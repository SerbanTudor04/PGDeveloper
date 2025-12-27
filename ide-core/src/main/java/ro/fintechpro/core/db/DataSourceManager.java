package ro.fintechpro.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSourceManager {

    // 1. The Single Instance
    private static final DataSourceManager INSTANCE = new DataSourceManager();

    private HikariDataSource dataSource;

    // 2. Private Constructor (prevents 'new DataSourceManager()')
    private DataSourceManager() {}

    // 3. Global Access Point
    public static DataSourceManager getInstance() {
        return INSTANCE;
    }

    public void connect(String host, int port, String database, String user, String password, boolean useSsl) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(user);
        config.setPassword(password);

        if (useSsl) {
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", "require");
            config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
        }

        config.setConnectionTimeout(5000);
        config.setMaximumPoolSize(5);

        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Not connected to any database. Call connect() first.");
        }
        return dataSource.getConnection();
    }

    public boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.execute("SELECT 1");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Optional: Get the name for the window title
    public String getConnectionInfoOrName() {
        if (dataSource != null) return dataSource.getJdbcUrl();
        return "Disconnected";
    }
}