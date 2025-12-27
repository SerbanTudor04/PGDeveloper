package ro.fintechpro.core.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ro.fintechpro.core.model.ConnectionProfile;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigService {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.pgdeveloper";
    private static final String CONFIG_FILE = CONFIG_DIR + "/connections.json";
    private final Gson gson = new Gson();

    public List<ConnectionProfile> loadConnections() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return new ArrayList<>();

        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, new TypeToken<List<ConnectionProfile>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveConnection(ConnectionProfile profile) {
        List<ConnectionProfile> current = loadConnections();
        current.add(profile);
        saveAll(current);
    }

    public void saveAll(List<ConnectionProfile> profiles) {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            try (Writer writer = new FileWriter(CONFIG_FILE)) {
                gson.toJson(profiles, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}