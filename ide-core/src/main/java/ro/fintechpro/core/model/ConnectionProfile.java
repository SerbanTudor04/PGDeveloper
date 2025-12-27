package ro.fintechpro.core.model;

public class ConnectionProfile {
    private String name;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password; // <--- NEW FIELD
    private boolean useSsl;

    public ConnectionProfile(String name, String host, int port, String database, String username, String password, boolean useSsl) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password; // <--- Save it
        this.useSsl = useSsl;
    }

    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; } // <--- Getter
    public boolean isUseSsl() { return useSsl; }

    @Override
    public String toString() {
        return name + " (" + username + "@" + host + ")";
    }
}