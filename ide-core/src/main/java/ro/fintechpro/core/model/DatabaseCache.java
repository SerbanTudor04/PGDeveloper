package ro.fintechpro.core.model;

import java.util.List;
import java.util.Map;

public record DatabaseCache(
        String connectionName,
        long lastIntrospectionTime,
        Map<String, SchemaCache> schemas
) {
    public record SchemaCache(
            String name,
            List<TableCache> tables,
            List<String> functions,
            List<String> procedures
    ) {}

    public record TableCache(String name, String type) {}
}