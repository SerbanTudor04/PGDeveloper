package ro.fintechpro.core.model;

public record SidebarItem(String label, String type, String schema, String name) {
    // Types
    public static final String TYPE_SCHEMA = "SCHEMA";
    public static final String TYPE_TABLE = "TABLE";
    public static final String TYPE_FUNCTION = "FUNCTION";
    public static final String TYPE_PROCEDURE = "PROCEDURE";
    public static final String TYPE_ROOT = "ROOT";
    public static final String TYPE_FOLDER = "FOLDER";

    @Override
    public String toString() {
        return label;
    }
}