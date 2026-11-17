package ua.airline.storage;

import java.nio.file.Path;
import java.util.Locale;

public enum DataFormat {
    JSON,
    YAML;

    public static DataFormat fromPath(Path file) {
        String name = file.getFileName() == null ? "" : file.getFileName().toString();
        int lastDot = name.lastIndexOf('.');
        String extension = lastDot >= 0 ? name.substring(lastDot + 1) : "";
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "json" -> JSON;
            case "yaml", "yml" -> YAML;
            default -> throw new IllegalArgumentException(
                    "Unsupported file extension: " + file);
        };
    }
}
