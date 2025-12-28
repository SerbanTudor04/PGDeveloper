package ro.fintechpro.core.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ro.fintechpro.core.model.DatabaseCache;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkspaceService {

    private static final String WORKSPACE_DIR = ".pgdev_workspace";
    private static final String STATE_FILE = "workspace.json";
    private static final String METADATA_PREFIX = "metadata_";
    private final Gson gson = new Gson();

    public record ConsoleState(String id, String name, String connectionName, String content) {}

    public WorkspaceService() {
        // Create workspace directory if it doesn't exist
        new File(WORKSPACE_DIR).mkdirs();
    }

    public void saveState(List<ConsoleState> consoles) {
        try (FileWriter writer = new FileWriter(new File(WORKSPACE_DIR, STATE_FILE))) {
            gson.toJson(consoles, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConsoleContent(String id, String content) {
        try {
            Files.writeString(Path.of(WORKSPACE_DIR, id + ".sql"), content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ConsoleState> loadState() {
        File file = new File(WORKSPACE_DIR, STATE_FILE);
        if (!file.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            List<ConsoleState> states = gson.fromJson(reader, new TypeToken<List<ConsoleState>>(){}.getType());

            // Load actual content from .sql files
            List<ConsoleState> loaded = new ArrayList<>();
            for (ConsoleState s : states) {
                String content = "";
                File sqlFile = new File(WORKSPACE_DIR, s.id() + ".sql");
                if (sqlFile.exists()) {
                    content = Files.readString(sqlFile.toPath());
                }
                loaded.add(new ConsoleState(s.id(), s.name(), s.connectionName(), content));
            }
            return loaded;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ConsoleState createNewConsole(String defaultConnection) {
        String id = UUID.randomUUID().toString();
        // Default name can be "console_1.sql", "console_2.sql" etc.
        return new ConsoleState(id, "console.sql", defaultConnection, "");
    }

    public void saveMetadata(DatabaseCache metadata) {
        // Sanitize connection name for filename
        String filename = METADATA_PREFIX + metadata.connectionName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
        try (FileWriter writer = new FileWriter(new File(WORKSPACE_DIR, filename))) {
            gson.toJson(metadata, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DatabaseCache loadMetadata(String connectionName) {
        String filename = METADATA_PREFIX + connectionName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
        File file = new File(WORKSPACE_DIR, filename);
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, DatabaseCache.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}