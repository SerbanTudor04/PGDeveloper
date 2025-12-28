// File: pgdeveloper/ide-core/src/main/java/ro/fintechpro/core/db/DataSourceManager.java
package ro.fintechpro.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ro.fintechpro.core.model.ConnectionProfile;
import ro.fintechpro.core.service.ConfigService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSourceManager {

    private static final DataSourceManager INSTANCE = new DataSourceManager();

    // Store definitions
    private final Map<String, ConnectionProfile> profiles = new ConcurrentHashMap<>();

    // Store active connection pools (Lazy loaded)
    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    // The currently active profile name (for legacy/global access)
    private String activeProfileName;
    private final ConfigService configService = new ConfigService();

    private DataSourceManager() {
        // Load profiles on startup
        try {
            var loadedProfiles = configService.loadConnections();
            for (ConnectionProfile p : loadedProfiles) {
                profiles.put(p.getName(), p);
            }

            if (!profiles.isEmpty()) {
                // Default to the first one found
                activeProfileName = profiles.keySet().iterator().next();
            }
        } catch (Exception e) {
            System.err.println("Failed to load profiles: " + e.getMessage());
        }
    }

    public static DataSourceManager getInstance() {
        return INSTANCE;
    }

    // --- Profile Management ---

    public Map<String, ConnectionProfile> getProfiles() {
        return profiles;
    }

    public void addProfile(ConnectionProfile profile) {
        profiles.put(profile.getName(), profile);
        persistProfiles();

        // If it's the first one, make it active
        if (activeProfileName == null) {
            activeProfileName = profile.getName();
        }
    }

    public void removeProfile(String name) {
        profiles.remove(name);
        persistProfiles();

        // Close pool if open
        HikariDataSource ds = dataSources.remove(name);
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }

        if (name.equals(activeProfileName)) {
            activeProfileName = profiles.isEmpty() ? null : profiles.keySet().iterator().next();
        }
    }

    private void persistProfiles() {
        configService.saveAll(new ArrayList<>(profiles.values()));
    }

    public void setActiveProfile(String name) {
        if (profiles.containsKey(name)) {
            this.activeProfileName = name;
        }
    }

    public String getActiveProfileName() {
        return activeProfileName;
    }

    // --- Connection Factory ---

    /**
     * Connects using the currently active profile.
     */
    public Connection getConnection() throws SQLException {
        if (activeProfileName == null) {
            throw new SQLException("No active connection profile selected.");
        }
        return getConnection(activeProfileName);
    }

    /**
     * Connects using a specific profile name (e.g. for a specific Tab).
     */
    public Connection getConnection(String profileName) throws SQLException {
        HikariDataSource ds = getOrInitDataSource(profileName);
        return ds.getConnection();
    }

    private synchronized HikariDataSource getOrInitDataSource(String profileName) throws SQLException {
        if (!profiles.containsKey(profileName)) {
            throw new SQLException("Profile not found: " + profileName);
        }

        // Return existing if open
        HikariDataSource existing = dataSources.get(profileName);
        if (existing != null && !existing.isClosed()) {
            return existing;
        }

        // Create new pool
        ConnectionProfile p = profiles.get(profileName);
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", p.getHost(), p.getPort(), p.getDatabase()));
            config.setUsername(p.getUsername());
            config.setPassword(p.getPassword());

            // Optional: Set pool name for debugging
            config.setPoolName("HikariPool-" + profileName);

            if (p.isUseSsl()) {
                config.addDataSourceProperty("ssl", "true");
                config.addDataSourceProperty("sslmode", "require");
                config.addDataSourceProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
            }

            config.setConnectionTimeout(5000);
            config.setMaximumPoolSize(5);

            HikariDataSource newDs = new HikariDataSource(config);
            dataSources.put(profileName, newDs);
            return newDs;

        } catch (Exception e) {
            throw new SQLException("Failed to initialize pool for profile " + profileName + ": " + e.getMessage(), e);
        }
    }

    // --- Utilities ---

    public boolean testConnection() {
        if (activeProfileName == null) return false;
        return testConnection(activeProfileName);
    }

    public boolean testConnection(String profileName) {
        try (Connection conn = getConnection(profileName);
             Statement stmt = conn.createStatement()) {
            return stmt.execute("SELECT 1");
        } catch (SQLException e) {
            return false;
        }
    }

    public String getConnectionInfoOrName() {
        if (activeProfileName != null) {
            ConnectionProfile p = profiles.get(activeProfileName);
            if (p != null) return activeProfileName + " [" + p.getHost() + "/" + p.getDatabase() + "]";
        }
        return "No Connection";
    }

    // Legacy support for manual connection without a profile object
    public void connect(String host, int port, String database, String user, String password, boolean useSsl) {
        // Create a temporary profile
        String tempName = "Temp-" + System.currentTimeMillis();
        ConnectionProfile temp = new ConnectionProfile(tempName, host, port, database, user, password, useSsl);
        profiles.put(tempName, temp);
        activeProfileName = tempName;
    }
}