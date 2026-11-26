package ua.airline.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.Passenger;
import ua.airline.repository.FlightRepository;
import ua.airline.repository.PassengerRepository;
import ua.airline.storage.JsonMappers;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ApiServerTest {
    private static final ObjectMapper MAPPER = JsonMappers.json();
    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 10, 0);
    private FlightRepository flights;
    private PassengerRepository passengers;
    private ApiServer server;
    private HttpClient client;

    @BeforeEach
    void startServer() throws Exception {
        flights = new FlightRepository();
        passengers = new PassengerRepository();
        flights.addAll(List.of(flight("PS-101", START, FlightStatus.SCHEDULED),
                flight("PS-102", START.plusHours(1), FlightStatus.DELAYED),
                flight("LO-103", START.plusHours(2), FlightStatus.SCHEDULED)));
        passengers.addAll(List.of(new Passenger("PP1", "Анна", LocalDate.of(1990, 1, 1)),
                new Passenger("PP2", "Олена", LocalDate.of(2000, 1, 1)),
                new Passenger("PP3", "Анна", LocalDate.of(2010, 1, 1))));
        server = new ApiServer(0, flights, passengers);
        server.start();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void stopServer() throws Exception {
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("Port zero binds an actual available port")
    void exposesAssignedPort() {
        assertTrue(server.port() > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"passengers", "flights"})
    @DisplayName("Collection GET returns a JSON array with a UTF-8 content type")
    void getsCollection(String entity) throws Exception {
        var response = send("GET", "/api/" + entity, null, null);

        assertEquals(200, response.statusCode());
        assertJsonContentType(response);
        assertTrue(tree(response).isArray());
        assertEquals(3, tree(response).size());
    }

    @ParameterizedTest
    @CsvSource({"passengers,PP1,passportSeriesNum", "flights,PS-101,flightNumber"})
    @DisplayName("Identity GET returns the stored object")
    void getsExistingObject(String entity, String id, String field) throws Exception {
        var response = send("GET", "/api/" + entity + "/" + id, null, null);

        assertEquals(200, response.statusCode());
        assertEquals(id, tree(response).path(field).asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"passengers", "flights"})
    @DisplayName("Missing identity returns a JSON ApiError")
    void getsMissingObject(String entity) throws Exception {
        String path = "/api/" + entity + "/missing";
        assertError(send("GET", path, null, null), 404, path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"passengers", "flights"})
    @DisplayName("POST creates an object and GET follows its Location")
    void postsValidObject(String entity) throws Exception {
        String body = payload(entity, "NEW-1");
        var response = send("POST", "/api/" + entity, body, "application/json; charset=UTF-8");

        assertEquals(201, response.statusCode());
        assertJsonContentType(response);
        assertEquals(MAPPER.readTree(body), tree(response));
        String location = response.headers().firstValue("Location").orElseThrow();
        assertEquals("/api/" + entity + "/NEW-1", location);
        var loaded = send("GET", location, null, null);
        assertEquals(200, loaded.statusCode());
        assertEquals(tree(response), tree(loaded));
    }

    @Test
    @DisplayName("Flight validation returns every violation including the custom annotation")
    void rejectsInvalidFlightWithAllViolations() throws Exception {
        Flight invalid = new Flight("", new Airport("KBP", "Київ"), new Airport("WAW", "Варшава"),
                START, START.minusMinutes(1), null);
        var response = send("POST", "/api/flights", MAPPER.writeValueAsString(invalid), "application/json");

        assertError(response, 400, "/api/flights");
        assertEquals(3, tree(response).path("violations").size());
        assertTrue(response.body().contains("Час прибуття має бути пізнішим за час вильоту"));
        assertTrue(response.body().contains("flightNumber"));
        assertTrue(response.body().contains("status"));
        assertEquals(3, flights.size());
    }

    @Test
    @DisplayName("Passenger validation returns all violations without storing the object")
    void rejectsInvalidPassengerWithAllViolations() throws Exception {
        var response = send("POST", "/api/passengers",
                "{\"passportSeriesNum\":\"\",\"fullName\":\"\",\"birthDate\":null}", "application/json");

        assertError(response, 400, "/api/passengers");
        assertEquals(3, tree(response).path("violations").size());
        assertEquals(3, passengers.size());
    }

    @ParameterizedTest
    @CsvSource({"passengers,PP1", "flights,PS-101"})
    @DisplayName("Duplicate POST returns Conflict and preserves repository size")
    void rejectsDuplicate(String entity, String id) throws Exception {
        var response = send("POST", "/api/" + entity, payload(entity, id), "application/json");

        assertError(response, 409, "/api/" + entity);
        assertEquals(3, size(entity));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "[]", "\"text\"", "null", "", "{} {}"})
    @DisplayName("Malformed JSON and incorrect root types return 400")
    void rejectsMalformedJson(String body) throws Exception {
        var response = send("POST", "/api/passengers", body, "application/json");

        assertError(response, 400, "/api/passengers");
        assertTrue(tree(response).path("violations").isEmpty());
        assertEquals(3, passengers.size());
    }

    @Test
    @DisplayName("Wrong field types are rejected rather than coerced")
    void rejectsWrongFieldType() throws Exception {
        var response = send("POST", "/api/passengers",
                "{\"passportSeriesNum\":123,\"fullName\":\"Ivan\",\"birthDate\":\"2000-01-01\"}", "application/json");

        assertError(response, 400, "/api/passengers");
        assertEquals(3, passengers.size());
    }

    @Test
    @DisplayName("Constructor validation failures become 400 instead of 500")
    void rejectsConstructorFailure() throws Exception {
        String body = payload("flights", "NEW-1").replace("KBP", "12A");

        assertError(send("POST", "/api/flights", body, "application/json"), 400, "/api/flights");
        assertEquals(3, flights.size());
    }

    @Test
    @DisplayName("Unknown JSON fields are rejected")
    void rejectsUnknownBodyField() throws Exception {
        var body = MAPPER.readTree(payload("passengers", "NEW-1"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("unknown", "value");

        assertError(send("POST", "/api/passengers", body.toString(), "application/json"), 400, "/api/passengers");
    }

    @ParameterizedTest
    @CsvSource({"POST,passengers,PP1", "POST,flights,PS-101", "PUT,passengers,PP1", "PUT,flights,PS-101"})
    @DisplayName("POST and PUT reject unsupported media types")
    void rejectsUnsupportedContentType(String method, String entity, String id) throws Exception {
        String path = "/api/" + entity + (method.equals("PUT") ? "/" + id : "");
        assertError(send(method, path, payload(entity, id), "text/plain"), 415, path);
    }

    @Test
    @DisplayName("Missing Content-Type returns 415")
    void rejectsMissingContentType() throws Exception {
        assertError(send("POST", "/api/passengers", payload("passengers", "NEW-1"), null),
                415, "/api/passengers");
    }

    @ParameterizedTest
    @CsvSource({"passengers,PP1", "flights,PS-101"})
    @DisplayName("PUT replaces an existing object and GET shows the change")
    void putsExistingObject(String entity, String id) throws Exception {
        String path = "/api/" + entity + "/" + id;
        String changed = payload(entity, id);
        var response = send("PUT", path, changed, "application/json");

        assertEquals(200, response.statusCode());
        assertEquals(MAPPER.readTree(changed), tree(response));
        assertEquals(MAPPER.readTree(changed), tree(send("GET", path, null, null)));
        assertEquals(3, size(entity));
    }

    @ParameterizedTest
    @ValueSource(strings = {"passengers", "flights"})
    @DisplayName("PUT does not insert a missing identity")
    void putsMissingObject(String entity) throws Exception {
        String path = "/api/" + entity + "/NEW-1";
        assertError(send("PUT", path, payload(entity, "NEW-1"), "application/json"), 404, path);
        assertEquals(3, size(entity));
    }

    @ParameterizedTest
    @CsvSource({"passengers,PP1", "flights,PS-101"})
    @DisplayName("PUT cannot change identity")
    void rejectsIdentityChange(String entity, String id) throws Exception {
        String path = "/api/" + entity + "/" + id;
        JsonNode before = tree(send("GET", path, null, null));

        assertError(send("PUT", path, payload(entity, "NEW-1"), "application/json"), 400, path);
        assertEquals(before, tree(send("GET", path, null, null)));
    }

    @ParameterizedTest
    @CsvSource({"passengers,PP1", "flights,PS-101"})
    @DisplayName("Invalid PUT leaves the stored object unchanged")
    void rejectsInvalidPut(String entity, String id) throws Exception {
        String path = "/api/" + entity + "/" + id;
        JsonNode before = tree(send("GET", path, null, null));
        String invalid = entity.equals("passengers")
                ? payload(entity, id).replace("Оновлене ім’я", "")
                : payload(entity, id).replace("CANCELLED", "UNKNOWN");

        assertError(send("PUT", path, invalid, "application/json"), 400, path);
        assertEquals(before, tree(send("GET", path, null, null)));
    }

    @ParameterizedTest
    @CsvSource({"passengers,PP1", "flights,PS-101"})
    @DisplayName("DELETE returns an empty 204 response and removes the object")
    void deletesObject(String entity, String id) throws Exception {
        String path = "/api/" + entity + "/" + id;
        var response = send("DELETE", path, null, null);

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        assertError(send("GET", path, null, null), 404, path);
        assertEquals(2, size(entity));
    }

    @ParameterizedTest
    @ValueSource(strings = {"passengers", "flights"})
    @DisplayName("DELETE of a missing identity returns 404")
    void deletesMissingObject(String entity) throws Exception {
        String path = "/api/" + entity + "/missing";
        assertError(send("DELETE", path, null, null), 404, path);
    }

    @ParameterizedTest
    @CsvSource({"DELETE,/api/passengers", "POST,/api/passengers/PP1", "PUT,/api/flights",
            "PATCH,/api/flights/PS-101", "OPTIONS,/api/passengers", "DELETE,/api/flights/stats"})
    @DisplayName("Unsupported method and path combinations return JSON 405")
    void rejectsUnsupportedMethod(String method, String path) throws Exception {
        assertError(send(method, path, "{}", "application/json"), 405, path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/unknown", "/api/passengers/PP1/extra", "/api/flights/PS-101/extra",
            "/api/flights/stats/extra", "/unknown"})
    @DisplayName("Unknown paths return JSON 404 instead of Jetty HTML")
    void rejectsUnknownPath(String path) throws Exception {
        assertError(send("GET", path, null, null), 404, path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"passengers", "flights"})
    @DisplayName("Ascending and descending sorting return reverse sequences")
    void sortsBothDirections(String entity) throws Exception {
        List<JsonNode> ascending = nodes(tree(send("GET", "/api/" + entity + "?order=ASC", null, null)));
        List<JsonNode> descending = nodes(tree(send("GET", "/api/" + entity + "?order=DESC", null, null)));

        assertEquals(ascending.reversed(), descending);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sort=unknown", "order=SIDEWAYS", "limit=-5", "limit=abc", "offset=-1",
            "offset=abc", "limit=999999999999", "unknown=value", "limit=1&limit=2", "sort=", "name=%20",
            "birthFrom=bad-date", "birthFrom=2010-01-01&birthTo=1990-01-01"})
    @DisplayName("Invalid collection parameters return an explained 400")
    void rejectsInvalidParameters(String query) throws Exception {
        var response = send("GET", "/api/passengers?" + query, null, null);

        assertError(response, 400, "/api/passengers");
        assertTrue(tree(response).path("message").asText().contains("параметр"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sort=unknown", "departureFrom=bad", "departureFrom=2027-01-01T00:00:00&departureTo=2026-01-01T00:00:00"})
    @DisplayName("Invalid flight sorting and date ranges return 400")
    void rejectsInvalidFlightParameters(String query) throws Exception {
        assertError(send("GET", "/api/flights?" + query, null, null), 400, "/api/flights");
    }

    @Test
    @DisplayName("Passenger text and date filters combine with sorting and pagination")
    void combinesPassengerFilters() throws Exception {
        String path = "/api/passengers?name=" + encode("Анна")
                + "&birthFrom=2000-01-01&birthTo=2015-01-01&sort=birthDate&order=DESC&limit=1&offset=0";
        var response = send("GET", path, null, null);

        assertEquals(200, response.statusCode());
        assertEquals(1, tree(response).size());
        assertEquals("PP3", tree(response).get(0).path("passportSeriesNum").asText());
    }

    @Test
    @DisplayName("Flight text and departure filters combine with sorting and pagination")
    void combinesFlightFilters() throws Exception {
        var response = send("GET", "/api/flights?number=PS-&departureFrom=2026-12-01T10:00:00"
                + "&departureTo=2026-12-01T12:00:00&sort=departureTime&order=DESC&offset=1&limit=1", null, null);

        assertEquals(200, response.statusCode());
        assertEquals(1, tree(response).size());
        assertEquals("PS-101", tree(response).get(0).path("flightNumber").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"limit=0", "offset=99", "birthFrom=2020-01-01"})
    @DisplayName("An empty subset is a successful empty array")
    void returnsEmptySubset(String query) throws Exception {
        var response = send("GET", "/api/passengers?" + query, null, null);
        assertEquals(200, response.statusCode());
        assertEquals(MAPPER.createArrayNode(), tree(response));
    }

    @Test
    @DisplayName("Flight stats use repository grouping and total duration")
    void getsFlightStats() throws Exception {
        var response = send("GET", "/api/flights/stats", null, null);
        assertEquals(200, response.statusCode());
        assertEquals(2, tree(response).path("byStatus").path("SCHEDULED").asInt());
        assertEquals(1, tree(response).path("byStatus").path("DELAYED").asInt());
        assertEquals(270, tree(response).path("totalDurationMinutes").asInt());
    }

    @Test
    @DisplayName("Passenger stats group records by birth year")
    void getsPassengerStats() throws Exception {
        var response = send("GET", "/api/passengers/stats", null, null);
        assertEquals(200, response.statusCode());
        assertEquals(3, tree(response).path("byBirthYear").size());
        assertEquals(1, tree(response).path("byBirthYear").path("2000").asInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ПП 10+?%&", "A/B", "A;B", "A%2FB"})
    @DisplayName("Encoded identities and Ukrainian text survive a POST and GET round trip")
    void preservesSpecialIdentity(String identity) throws Exception {
        String body = payload("passengers", identity);
        var response = send("POST", "/api/passengers", body, "application/json");

        assertEquals(201, response.statusCode());
        String location = response.headers().firstValue("Location").orElseThrow();
        var loaded = send("GET", location, null, null);
        assertEquals(200, loaded.statusCode());
        assertJsonContentType(loaded);
        assertEquals(MAPPER.readTree(body), tree(loaded));
    }

    @ParameterizedTest
    @ValueSource(strings = {"passengers", "flights"})
    @DisplayName("Concurrent POST requests create exactly one object for the same identity")
    void postsDuplicateConcurrently(String entity) throws Exception {
        server.stop();
        flights = new FlightRepository();
        passengers = new PassengerRepository();
        server = new ApiServer(0, flights, passengers);
        server.start();
        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(client.sendAsync(request("POST", "/api/" + entity,
                    payload(entity, "SAME-1"), "application/json"), HttpResponse.BodyHandlers.ofString()));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new)).get(10, java.util.concurrent.TimeUnit.SECONDS);
        List<Integer> statuses = futures.stream().map(f -> f.join().statusCode()).toList();

        assertEquals(1, statuses.stream().filter(status -> status == 201).count());
        assertEquals(9, statuses.stream().filter(status -> status == 409).count());
        assertEquals(1, size(entity));
    }

    @ParameterizedTest
    @CsvSource({"passengers,PP1", "flights,PS-101"})
    @DisplayName("Concurrent add replace delete and snapshot reads remain consistent")
    void handlesConcurrentMixedRequests(String entity, String id) throws Exception {
        List<CompletableFuture<HttpResponse<String>>> creates = new ArrayList<>();
        List<CompletableFuture<HttpResponse<String>>> updates = new ArrayList<>();
        List<CompletableFuture<HttpResponse<String>>> deletes = new ArrayList<>();
        List<CompletableFuture<HttpResponse<String>>> reads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            creates.add(client.sendAsync(request("POST", "/api/" + entity,
                    payload(entity, "NEW-" + i), "application/json"), HttpResponse.BodyHandlers.ofString()));
            updates.add(client.sendAsync(request("PUT", "/api/" + entity + "/" + id,
                    payload(entity, id), "application/json"), HttpResponse.BodyHandlers.ofString()));
            deletes.add(client.sendAsync(request("DELETE", "/api/" + entity + "/" + id,
                    null, null), HttpResponse.BodyHandlers.ofString()));
            reads.add(client.sendAsync(request("GET", "/api/" + entity, null, null),
                    HttpResponse.BodyHandlers.ofString()));
        }
        List<CompletableFuture<HttpResponse<String>>> all = new ArrayList<>();
        all.addAll(creates);
        all.addAll(updates);
        all.addAll(deletes);
        all.addAll(reads);
        CompletableFuture.allOf(all.toArray(CompletableFuture<?>[]::new)).get(10, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(creates.stream().allMatch(f -> f.join().statusCode() == 201));
        assertTrue(updates.stream().allMatch(f -> List.of(200, 404).contains(f.join().statusCode())));
        assertTrue(deletes.stream().allMatch(f -> List.of(204, 404).contains(f.join().statusCode())));
        assertTrue(reads.stream().allMatch(f -> f.join().statusCode() == 200));
        assertEquals(12, size(entity));
        assertError(send("GET", "/api/" + entity + "/" + id, null, null), 404, "/api/" + entity + "/" + id);
    }

    private HttpResponse<String> send(String method, String path, String body, String contentType) throws Exception {
        return client.send(request(method, path, body, contentType), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpRequest request(String method, String path, String body, String contentType) {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path))
                .timeout(Duration.ofSeconds(5));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        return builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
    }

    private static void assertError(HttpResponse<String> response, int status, String path) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        assertJsonContentType(response);
        JsonNode error = tree(response);
        assertEquals(status, error.path("status").asInt());
        assertEquals(path, error.path("path").asText());
        assertFalse(error.path("error").asText().isBlank());
        assertFalse(error.path("message").asText().isBlank());
        assertTrue(error.path("violations").isArray());
        assertFalse(error.has("stackTrace"));
        if (status != 400) {
            assertTrue(error.path("violations").isEmpty());
        }
    }

    private static void assertJsonContentType(HttpResponse<String> response) {
        String type = response.headers().firstValue("Content-Type").orElseThrow().toLowerCase(java.util.Locale.ROOT);
        assertTrue(type.contains("application/json"), type);
        assertTrue(type.contains("charset=utf-8"), type);
    }

    private static JsonNode tree(HttpResponse<String> response) throws Exception {
        return MAPPER.readTree(response.body());
    }

    private static List<JsonNode> nodes(JsonNode array) {
        List<JsonNode> result = new ArrayList<>();
        array.forEach(result::add);
        return result;
    }

    private static String payload(String entity, String identity) throws Exception {
        return MAPPER.writeValueAsString(entity.equals("passengers")
                ? new Passenger(identity, "Оновлене ім’я", LocalDate.of(2001, 5, 12))
                : flight(identity, START.plusDays(1), FlightStatus.CANCELLED));
    }

    private int size(String entity) {
        return entity.equals("passengers") ? passengers.size() : flights.size();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Flight flight(String number, LocalDateTime departure, FlightStatus status) {
        return new Flight(number, new Airport("KBP", "Київ"), new Airport("WAW", "Варшава"),
                departure, departure.plusMinutes(90), status);
    }
}
