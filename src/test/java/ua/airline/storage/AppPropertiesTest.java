package ua.airline.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppPropertiesTest {
    @TempDir
    Path tempDir;

    @Test
    void readsPathAndNamesMissingKeyInException() throws IOException {
        Path file = Files.writeString(tempDir.resolve("app.properties"), "data.passengers=samples/p.json\n");
        try (InputStream input = Files.newInputStream(file)) {
            AppProperties properties = new AppProperties(input);

            assertEquals(Path.of("samples/p.json"), properties.requiredPath("data.passengers"));
            var error = assertThrows(IllegalArgumentException.class,
                    () -> properties.requiredPath("data.flights"));
            assertTrue(error.getMessage().contains("data.flights"));
        }
    }
}
