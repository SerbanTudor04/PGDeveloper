package ro.fintechpro.core.model;

public class ConnectionProfile {
    private String name;
    private String host;
    private int port;
    private String database;
    private String username;
    private boolean useSsl;

    // We intentionally don't save passwords for security in this simple version
    // You can add it, but it's better to ask for it every time or use a keyring later.

    public ConnectionProfile(String name, String host, int port, String database, String username, boolean useSsl) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.useSsl = useSsl;
    }

    // Getters
    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public boolean isUseSsl() { return useSsl; }

    @Override
    public String toString() {
        return name + " (" + username + "@" + host + ")";
    }
}