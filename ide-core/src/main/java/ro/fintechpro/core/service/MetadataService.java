package ro.fintechpro.core.service;

import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.model.DatabaseCache;
import ro.fintechpro.core.model.DatabaseCache.SchemaCache;
import ro.fintechpro.core.model.DatabaseCache.TableCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MetadataService {

    private final DataSourceManager dbManager;
    private DatabaseCache activeCache; // <--- Stores the in-memory model

    public MetadataService(DataSourceManager dbManager) {
        this.dbManager = dbManager;
    }

    // --- New Introspection Logic ---

    public DatabaseCache introspect(String connectionName, DatabaseCache previousCache) throws Exception {
        Map<String, SchemaCache> newSchemasMap = new HashMap<>();

        // 1. Get list of schemas from DB
        List<String> currentSchemaNames = fetchSchemasFromDb();

        for (String schemaName : currentSchemaNames) {
            // 2. Fetch basic object lists
            List<TableCache> currentTables = fetchTablesFromDb(schemaName);
            List<String> currentFunctions = fetchFunctionsFromDb(schemaName);
            List<String> currentProcedures = fetchProceduresFromDb(schemaName);

            // 3. Check for changes (Reuse old cache object if identical)
            SchemaCache previousSchema = (previousCache != null && previousCache.schemas() != null)
                    ? previousCache.schemas().get(schemaName)
                    : null;

            if (previousSchema != null && isSchemaUnchanged(previousSchema, currentTables, currentFunctions, currentProcedures)) {
                newSchemasMap.put(schemaName, previousSchema);
            } else {
                newSchemasMap.put(schemaName, new SchemaCache(schemaName, currentTables, currentFunctions, currentProcedures));
            }
        }

        // 4. Update Internal State
        this.activeCache = new DatabaseCache(connectionName, System.currentTimeMillis(), newSchemasMap);
        return this.activeCache;
    }

    // Helper: Detect changes
    private boolean isSchemaUnchanged(SchemaCache old, List<TableCache> tables, List<String> funcs, List<String> procs) {
        if (old.tables().size() != tables.size()) return false;
        if (old.functions().size() != funcs.size()) return false;
        if (!old.procedures().equals(procs)) return false;

        // Simple name/type check for tables (containsAll assumes unique names)
        return old.functions().containsAll(funcs) && old.tables().containsAll(tables);
    }

    // --- Public API (Now served from Cache if available) ---

    public List<String> getSchemas() throws Exception {
        if (activeCache != null) {
            return new ArrayList<>(activeCache.schemas().keySet());
        }
        return fetchSchemasFromDb();
    }

    public List<DbObject> getTables(String schema) throws Exception {
        if (activeCache != null) {
            SchemaCache sc = activeCache.schemas().get(schema);
            if (sc != null) {
                return sc.tables().stream()
                        .map(t -> new DbObject(t.name(), t.type()))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }
        return fetchTablesFromDb(schema).stream()
                .map(t -> new DbObject(t.name(), t.type()))
                .collect(Collectors.toList());
    }

    public List<String> getProcedures(String schema) throws Exception {
        if (activeCache != null) {
            SchemaCache sc = activeCache.schemas().get(schema);
            return sc != null ? sc.procedures() : new ArrayList<>();
        }
        return fetchProceduresFromDb(schema);
    }

    public List<String> getFunctions(String schema) throws Exception {
        if (activeCache != null) {
            SchemaCache sc = activeCache.schemas().get(schema);
            return sc != null ? sc.functions() : new ArrayList<>();
        }
        return fetchFunctionsFromDb(schema);
    }

    // --- Internal JDBC calls (Renamed from original public methods) ---

    private List<String> fetchSchemasFromDb() throws Exception {
        List<String> schemas = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getSchemas()) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (!schema.equals("information_schema") && !schema.startsWith("pg_")) {
                    schemas.add(schema);
                }
            }
        }
        return schemas;
    }

    private List<TableCache> fetchTablesFromDb(String schema) throws Exception {
        List<TableCache> tables = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, schema, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                tables.add(new TableCache(rs.getString("TABLE_NAME"), rs.getString("TABLE_TYPE")));
            }
        }
        return tables;
    }

    private List<String> fetchProceduresFromDb(String schema) throws Exception {
        List<String> procs = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getProcedures(null, schema, "%")) {
            while (rs.next()) {
                procs.add(rs.getString("PROCEDURE_NAME"));
            }
        }
        return procs;
    }

    private List<String> fetchFunctionsFromDb(String schema) throws Exception {
        List<String> functions = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getFunctions(null, schema, "%")) {
            while (rs.next()) {
                functions.add(rs.getString("FUNCTION_NAME"));
            }
        }
        return functions;
    }

    // Legacy support record
    public record DbObject(String name, String type) {}


    public record ColumnInfo(String name, String type, int size, boolean isNullable) {}
    public record IndexInfo(String name, boolean isUnique) {}

    public List<ColumnInfo> getColumns(String schema, String table) throws Exception {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, schema, table, null)) {
            while (rs.next()) {
                columns.add(new ColumnInfo(
                        rs.getString("COLUMN_NAME"),
                        rs.getString("TYPE_NAME"),
                        rs.getInt("COLUMN_SIZE"),
                        "YES".equals(rs.getString("IS_NULLABLE"))
                ));
            }
        }
        return columns;
    }

    public List<IndexInfo> getIndexes(String schema, String table) throws Exception {
        List<IndexInfo> indexes = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             // boolean unique, boolean approximate
             ResultSet rs = conn.getMetaData().getIndexInfo(null, schema, table, false, false)) {
            while (rs.next()) {
                String idxName = rs.getString("INDEX_NAME");
                if (idxName != null) { // Some rows are stats
                    boolean unique = !rs.getBoolean("NON_UNIQUE");
                    // Simple dedup logic or list could be used
                    if (indexes.stream().noneMatch(i -> i.name().equals(idxName))) {
                        indexes.add(new IndexInfo(idxName, unique));
                    }
                }
            }
        }
        return indexes;
    }

    // --- NEW: Routine Details ---

    public String getRoutineSource(String schema, String name) throws Exception {
        // Postgres specific way to get CREATE OR REPLACE FUNCTION ...
        // We use string formatting for schema.name, but binding for the OID lookup would be safer
        // if we had the OID. For now, we rely on regproc casting.
        String sql = "SELECT pg_get_functiondef((? || '.' || ?)::regproc)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schema);
            stmt.setString(2, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return "-- Source not found or not supported for this object type.";
    }
}