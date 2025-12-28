package ro.fintechpro.core.service;

import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.model.DatabaseCache;
import ro.fintechpro.core.model.DatabaseCache.SchemaCache;
import ro.fintechpro.core.model.DatabaseCache.TableCache;

import java.sql.Connection;
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
}