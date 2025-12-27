package ro.fintechpro.core.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LocalIndexService {

    // Runs completely in memory (super fast, cleared on restart)
    // If you want persistence, change to "jdbc:h2:./pgdev_index"
    private static final String JDBC_URL = "jdbc:h2:mem:pgdev_index;DB_CLOSE_DELAY=-1";

    public record SearchResult(String name, String type, String schema, String parent) {}

    public LocalIndexService() {
        initDb();
    }

    private void initDb() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            try (Statement stmt = conn.createStatement()) {
                // Create a fast table for searching
                stmt.execute("CREATE TABLE IF NOT EXISTS search_index (" +
                        "name VARCHAR, " +
                        "type VARCHAR, " +
                        "schema VARCHAR, " +
                        "parent VARCHAR)");

                // Index specifically for the 'name' column for instant search
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_name ON search_index(name)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearIndex() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE search_index");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Batch insert for performance
    public void indexItems(List<SearchResult> items) {
        String sql = "INSERT INTO search_index (name, type, schema, parent) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Start transaction

            for (SearchResult item : items) {
                pstmt.setString(1, item.name());
                pstmt.setString(2, item.type());
                pstmt.setString(3, item.schema());
                pstmt.setString(4, item.parent());
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit(); // Commit all at once

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<SearchResult> search(String query) {
        List<SearchResult> results = new ArrayList<>();
        // Use ILIKE for case-insensitive search in H2
        String sql = "SELECT * FROM search_index WHERE name ILIKE ? LIMIT 50";

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + query + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new SearchResult(
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getString("schema"),
                            rs.getString("parent")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
}