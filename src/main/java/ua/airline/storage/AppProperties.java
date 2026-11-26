package ua.airline.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class AppProperties {
    private final Properties values = new Properties();

    public AppProperties(InputStream input) throws IOException {
        values.load(input);
    }

    public Path requiredPath(String key) {
        return Path.of(requiredValue(key));
    }

    public int requiredPort(String key) {
        String value = requiredValue(key);
        try {
            int port = Integer.parseInt(value);
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("Port for property " + key + " must be between 0 and 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Property " + key + " must contain an integer port", e);
        }
    }

    private String requiredValue(String key) {
        String value = values.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing value for property " + key);
        }
        return value.trim();
    }
}
