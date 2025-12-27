package ro.fintechpro.core.service;

import ro.fintechpro.core.db.DataSourceManager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class MetadataService {

    private final DataSourceManager dbManager;

    public MetadataService(DataSourceManager dbManager) {
        this.dbManager = dbManager;
    }

    // Simple Record to hold DB Object info
    public record DbObject(String name, String type) {}

    public List<String> getSchemas() throws Exception {
        List<String> schemas = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getSchemas()) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                // Filter out system schemas for a cleaner view
                if (!schema.equals("information_schema") && !schema.startsWith("pg_")) {
                    schemas.add(schema);
                }
            }
        }
        return schemas;
    }

    public List<DbObject> getTables(String schema) throws Exception {
        List<DbObject> tables = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, schema, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                String type = rs.getString("TABLE_TYPE");
                tables.add(new DbObject(name, type));
            }
        }
        return tables;
    }

    // Functions are trickier in standard JDBC, but this is a solid start
    public List<String> getProcedures(String schema) throws Exception {
        List<String> procs = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getProcedures(null, schema, "%")) {
            while (rs.next()) {
                procs.add(rs.getString("PROCEDURE_NAME"));
            }
        }
        return procs;
    }
}