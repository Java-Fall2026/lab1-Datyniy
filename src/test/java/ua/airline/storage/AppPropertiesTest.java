package ua.airline.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppPropertiesTest {
    @Test
    @DisplayName("Reads Configured Path")
    void readsConfiguredPath() throws IOException {
        AppProperties properties = properties("data.passengers=samples/p.json\n");

        assertEquals(Path.of("samples/p.json"), properties.requiredPath("data.passengers"));
    }

    @Test
    @DisplayName("Trims Configured Path")
    void trimsConfiguredPath() throws IOException {
        AppProperties properties = properties("data.passengers=  samples/p.json  \n");

        assertEquals(Path.of("samples/p.json"), properties.requiredPath("data.passengers"));
    }

    @Test
    @DisplayName("Names Missing Key In Exception")
    void namesMissingKeyInException() throws IOException {
        AppProperties properties = properties("data.passengers=samples/p.json\n");

        var error = assertThrows(IllegalArgumentException.class,
                () -> properties.requiredPath("data.flights"));

        assertTrue(error.getMessage().contains("data.flights"));
    }

    @ParameterizedTest(name = "Blank configuration value: [{0}]")
    @DisplayName("Rejects Blank Configured Path")
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankConfiguredPath(String value) throws IOException {
        AppProperties properties = properties("data.passengers=" + value + "\n");

        var error = assertThrows(IllegalArgumentException.class,
                () -> properties.requiredPath("data.passengers"));

        assertTrue(error.getMessage().contains("data.passengers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "8080", "65535", " 8081 "})
    @DisplayName("Reads a valid port including port zero")
    void readsValidPort(String value) throws IOException {
        assertEquals(Integer.parseInt(value.trim()), properties("server.port=" + value).requiredPort("server.port"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "-1", "65536", "2147483648"})
    @DisplayName("Rejects invalid ports and names the configuration key")
    void rejectsInvalidPort(String value) throws IOException {
        AppProperties properties = properties("server.port=" + value);
        var error = assertThrows(IllegalArgumentException.class, () -> properties.requiredPort("server.port"));
        assertTrue(error.getMessage().contains("server.port"));
    }

    @Test
    @DisplayName("Rejects a missing port without using a default")
    void rejectsMissingPort() throws IOException {
        AppProperties properties = properties("");
        var error = assertThrows(IllegalArgumentException.class, () -> properties.requiredPort("server.port"));
        assertTrue(error.getMessage().contains("server.port"));
    }

    private static AppProperties properties(String text) throws IOException {
        try (var input = new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1))) {
            return new AppProperties(input);
        }
    }
}
