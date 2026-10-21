package ua.airline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvLoader<T> {

    private static final Logger log = LoggerFactory.getLogger(CsvLoader.class);

    @FunctionalInterface
    public interface RowParser<T> {
        T parse(String line, int lineNumber) throws InvalidDataException;
    }

    private final String expectedHeader;
    private final RowParser<T> parser;

    public CsvLoader(String expectedHeader, RowParser<T> parser) {
        this.expectedHeader = expectedHeader;
        this.parser = parser;
    }

    public List<T> load(Path path) {
        List<T> entries = new ArrayList<>();
        int rejectedRows = 0;

        log.info("Reading CSV file {}", path);

        try (BufferedReader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {

            String header = reader.readLine();
            if (!expectedHeader.equals(header)) {
                throw new InvalidDataException(
                        "Line 1: expected header '" + expectedHeader
                                + "', got: " + header
                );
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                try {
                    entries.add(parser.parse(line, lineNumber));
                } catch (InvalidDataException e) {
                    rejectedRows++;
                    log.warn("Skipping invalid CSV row: {}", e.getMessage(), e);
                }
            }

        } catch (InvalidDataException e) {
            log.error("Invalid CSV header in {}", path, e);
        } catch (IOException e) {
            log.error("Failed to read CSV file {}", path, e);
        } finally {
            log.info(
                    "Loaded {} entries from {}; rejected {} rows",
                    entries.size(), path, rejectedRows
            );
        }

        return entries;
    }
}
