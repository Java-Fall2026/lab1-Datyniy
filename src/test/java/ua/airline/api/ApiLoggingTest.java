package ua.airline.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import ua.airline.model.Flight;
import ua.airline.repository.FlightRepository;
import ua.airline.repository.PassengerRepository;
import ua.airline.storage.JsonMappers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiLoggingTest {
    private ApiServer server;
    private HttpClient client;

    @BeforeEach
    void startServer() throws Exception {
        server = new ApiServer(0, new FlightRepository(), new PassengerRepository());
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() throws Exception {
        try {
            client.close();
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("Successful requests are logged once at INFO with query and elapsed time")
    void logsSuccessfulRequest() throws Exception {
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class)) {
            var response = get("/api/flights?limit=1");

            assertEquals(200, response.statusCode());
            assertEquals(1, capture.events().size());
            var event = capture.events().getFirst();
            assertEquals(Level.INFO, event.getLevel());
            assertTrue(event.getFormattedMessage().matches("GET /api/flights\\?limit=1 -> 200 \\(\\d+ ms\\)"));
            assertNull(event.getThrowableProxy());
        }
    }

    @Test
    @DisplayName("Client errors are logged once at WARN")
    void logsClientError() throws Exception {
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class)) {
            assertEquals(404, get("/api/unknown").statusCode());

            assertEquals(1, capture.events().size());
            assertEquals(Level.WARN, capture.events().getFirst().getLevel());
            assertTrue(capture.events().getFirst().getFormattedMessage().contains("-> 404"));
            assertNull(capture.events().getFirst().getThrowableProxy());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Unexpected errors are masked and logged once with the final 500 status")
    void logsAndMasksUnexpectedFailure(boolean illegalArgument) throws Exception {
        server.stop();
        FlightRepository broken = new FlightRepository() {
            @Override
            public List<Flight> findAll() {
                if (illegalArgument) {
                    throw new IllegalArgumentException("secret diagnostic details");
                }
                throw new IllegalStateException("secret diagnostic details");
            }
        };
        server = new ApiServer(0, broken, new PassengerRepository());
        server.start();
        try (LogCapture requests = new LogCapture(RequestLoggingFilter.class);
             LogCapture errors = new LogCapture(ErrorHandlingFilter.class)) {
            var response = get("/api/flights");
            var body = JsonMappers.json().readTree(response.body());

            assertEquals(500, response.statusCode());
            assertEquals("Internal Server Error", body.path("error").asText());
            assertEquals("Внутрішня помилка сервера", body.path("message").asText());
            assertEquals("/api/flights", body.path("path").asText());
            assertTrue(body.path("violations").isEmpty());
            assertFalse(response.body().contains("secret"));
            assertFalse(response.body().contains("IllegalStateException"));
            assertFalse(response.body().contains("stackTrace"));
            assertEquals(1, errors.events().size());
            assertEquals(Level.ERROR, errors.events().getFirst().getLevel());
            assertNotNull(errors.events().getFirst().getThrowableProxy());
            assertEquals(1, requests.events().size());
            assertEquals(Level.ERROR, requests.events().getFirst().getLevel());
            assertTrue(requests.events().getFirst().getFormattedMessage().contains("-> 500"));
            assertNull(requests.events().getFirst().getThrowableProxy());
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path))
                .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static final class LogCapture implements AutoCloseable {
        private final Logger logger;
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

        LogCapture(Class<?> type) {
            logger = (Logger) LoggerFactory.getLogger(type);
            appender.start();
            logger.addAppender(appender);
        }

        List<ILoggingEvent> events() {
            return appender.list;
        }

        @Override
        public void close() {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
