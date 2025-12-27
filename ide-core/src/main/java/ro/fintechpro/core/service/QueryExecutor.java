package ro.fintechpro.core.service;

import ro.fintechpro.core.db.DataSourceManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryExecutor {

    private final DataSourceManager dbManager = DataSourceManager.getInstance();

    // A simple container for the result
    public record QueryResult(
            boolean isResultSet,
            List<String> columns,
            List<List<Object>> data, // The rows
            int updateCount,
            String message
    ) {}

    public QueryResult execute(String sql) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // execute() returns true if the first result is a ResultSet
            boolean hasResultSet = stmt.execute(sql);

            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    // 1. Get Column Names
                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }

                    // 2. Get Data Rows
                    List<List<Object>> data = new ArrayList<>();
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        data.add(row);
                    }

                    return new QueryResult(true, columns, data, 0, "Query executed successfully.");
                }
            } else {
                // It was an UPDATE / INSERT / DELETE / DDL
                int count = stmt.getUpdateCount();
                return new QueryResult(false, null, null, count, "Statement executed. Rows affected: " + count);
            }
        }
    }
}