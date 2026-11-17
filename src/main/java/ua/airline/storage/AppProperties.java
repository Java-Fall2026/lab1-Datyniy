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
        String value = values.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing value for property " + key);
        }
        return Path.of(value.trim());
    }
}
