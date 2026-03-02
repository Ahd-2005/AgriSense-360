package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads API keys from config.properties (excluded from Git).
 * Copy config.properties.example → config.properties and fill your keys.
 */
public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream is = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("[AppConfig] config.properties not found. Copy config.properties.example and fill your keys.");
            }
        } catch (IOException e) {
            System.err.println("[AppConfig] Failed to load config.properties: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "");
    }
}
