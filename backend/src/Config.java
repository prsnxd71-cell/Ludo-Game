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

        // 1. Check environment variables first (Cloud / Render / Docker)
        String envVal = getFromEnv(key);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }

        // 2. Check local config.properties file (Local development)
        String val = properties.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 3. Fallback to default
        return defaultValue;
    }

    private static String getFromEnv(String key) {
        if (key == null) return null;

        // Direct match
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) return val;

        // Uppercase with underscores (e.g. "db.host" -> "DB_HOST")
        String standardEnvName = key.toUpperCase().replace('.', '_');
        val = System.getenv(standardEnvName);
        if (val != null && !val.trim().isEmpty()) return val;

        // Cloud provider aliases (Render, Railway, Heroku, Aiven, etc.)
        switch (key) {
            case "server.port":
                val = System.getenv("PORT");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("SERVER_PORT");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
            case "db.host":
                val = System.getenv("MYSQLHOST");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("MYSQL_HOST");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
            case "db.port":
                val = System.getenv("MYSQLPORT");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("MYSQL_PORT");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
            case "db.name":
                val = System.getenv("MYSQLDATABASE");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("MYSQL_DATABASE");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
            case "db.user":
                val = System.getenv("MYSQLUSER");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("MYSQL_USER");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
            case "db.password":
                val = System.getenv("MYSQLPASSWORD");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("MYSQL_PASSWORD");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
            case "db.url":
                val = System.getenv("MYSQL_URL");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("DATABASE_URL");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("DB_URL");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
            case "db.use_ssl":
                val = System.getenv("DB_USE_SSL");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("DB_SSL");
                if (val != null && !val.trim().isEmpty()) return val;
                val = System.getenv("MYSQL_SSL");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
            case "minimoth.api_key":
                val = System.getenv("MINIMOTH_API_KEY");
                if (val != null && !val.trim().isEmpty()) return val;
                break;
        }

        return null;
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
    public static String getDbUrl() { return get("db.url", null); }
    public static String getDbUseSsl() {
        String ssl = get("db.use_ssl", null);
        if (ssl != null) return ssl;
        String host = getDbHost();
        return ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) ? "false" : "true";
    }
    public static String getMiniMothApiKey() { return get("minimoth.api_key", ""); }
    public static int getServerPort() { return getInt("server.port", 8080); }
}

