package ua.airline.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FileStorage<T> {
    private static final ObjectMapper JSON = JsonMappers.json();
    private static final ObjectMapper YAML = JsonMappers.yaml();

    private final Class<T> itemType;

    public FileStorage(Class<T> itemType) {
        this.itemType = itemType;
    }

    public List<T> load(Path file) throws IOException {
        ArrayNode records = readRecords(file);
        List<T> items = new ArrayList<>(records.size());
        for (JsonNode record : records) {
            items.add(readItem(file, record));
        }
        return items;
    }

    public void save(Path file, List<T> items) throws IOException {
        mapperFor(file).writerWithDefaultPrettyPrinter().writeValue(file.toFile(), items);
    }

    ArrayNode readRecords(Path file) throws IOException {
        JsonNode root = mapperFor(file).readTree(file.toFile());
        if (root instanceof ArrayNode records) {
            return records;
        }
        throw new IOException("Expected an array of records");
    }

    T readItem(Path file, JsonNode record) throws JsonProcessingException {
        return mapperFor(file).treeToValue(record, itemType);
    }

    private static ObjectMapper mapperFor(Path file) {
        return switch (DataFormat.fromPath(file)) {
            case JSON -> JSON;
            case YAML -> YAML;
        };
    }

}
