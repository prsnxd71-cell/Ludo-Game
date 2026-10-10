import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads local configuration from config.properties.
 * Keeps credentials and API keys out of code and source control.
 */
public class Config {
    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;
        File file = new File("config.properties");
        if (!file.exists()) {
            file = new File("backend/config.properties");
        }
        if (!file.exists()) {
            file = new File("../config.properties");
        }
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
                System.out.println("[CONFIG] Successfully loaded " + file.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[CONFIG] Error reading config.properties: " + e.getMessage());
            }
        } else {
            System.out.println("[CONFIG] config.properties not found on disk, using defaults.");
        }
        loaded = true;
    }

    public static String get(String key, String defaultValue) {
        load();
        String val = properties.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        return val.trim();
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getDbHost() { return get("db.host", "localhost"); }
    public static String getDbPort() { return get("db.port", "3306"); }
    public static String getDbName() { return get("db.name", "ludo_db"); }
    public static String getDbUser() { return get("db.user", "root"); }
    public static String getDbPassword() { return get("db.password", ""); }
    public static String getMiniMothApiKey() { return get("minimoth.api_key", ""); }
    public static int getServerPort() { return getInt("server.port", 8080); }
}

