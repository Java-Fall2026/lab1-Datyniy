package ua.airline.api;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.airline.model.Flight;
import ua.airline.repository.FlightRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FlightServlet extends HttpServlet {
    private final FlightRepository repository;
    private final JsonSupport json;
    private final Map<String, Comparator<Flight>> comparators = Map.of(
            "flightNumber", Comparator.naturalOrder(),
            "departureTime", Comparator.comparing(Flight::getDepartureTime).thenComparing(Flight::getFlightNumber),
            "status", Comparator.comparing(Flight::getStatus).thenComparing(Flight::getFlightNumber));

    public FlightServlet(FlightRepository repository, JsonSupport json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiPath path = ApiPath.parse(request);
        if (!request.getMethod().equals("GET") && request.getQueryString() != null) {
            throw ApiException.invalidParameter("query", "цей метод не підтримує параметри запиту");
        }
        if (!path.collection()) {
            new QueryParameters(request, Set.of());
        }
        switch (request.getMethod()) {
            case "GET" -> get(request, response, path);
            case "POST" -> {
                path.requireCollection();
                Flight flight = json.readValid(request, Flight.class);
                String identity = repository.identityOf(flight);
                if (identity.equals("stats")) {
                    throw new ApiException(400, "Identity stats is reserved",
                            "Ідентичність stats зарезервована для статистики");
                }
                repository.add(flight);
                response.setHeader("Location", ApiPath.location(request, identity));
                json.write(response, 201, flight);
            }
            case "PUT" -> {
                String identity = path.requireIdentity();
                Flight flight = json.readValid(request, Flight.class);
                if (!identity.equals(repository.identityOf(flight))) {
                    throw new ApiException(400, "Body identity does not match path",
                            "Ідентичність у тілі не збігається зі шляхом");
                }
                if (!repository.replace(flight)) {
                    throw ApiException.notFound();
                }
                json.write(response, 200, flight);
            }
            case "DELETE" -> {
                if (!repository.removeByIdentity(path.requireIdentity())) {
                    throw ApiException.notFound();
                }
                response.setStatus(204);
            }
            default -> throw ApiException.methodNotAllowed();
        }
    }

    private void get(HttpServletRequest request, HttpServletResponse response, ApiPath path) throws IOException {
        if (path.stats()) {
            json.write(response, 200, Map.of("byStatus", repository.countByStatus(),
                    "totalDurationMinutes", repository.totalDurationMinutes()));
        } else if (!path.collection()) {
            json.write(response, 200, repository.findByIdentity(path.identity()).orElseThrow(ApiException::notFound));
        } else {
            QueryParameters query = new QueryParameters(request,
                    Set.of("number", "departureFrom", "departureTo", "sort", "order", "limit", "offset"));
            List<Flight> result = query.text("number") == null
                    ? repository.findAll() : repository.findByNumberContaining(query.text("number"));
            if (query.hasRange("departureFrom", "departureTo")) {
                LocalDateTime from = query.dateTime("departureFrom", LocalDateTime.MIN);
                LocalDateTime to = query.dateTime("departureTo", LocalDateTime.MAX);
                if (from.isAfter(to)) {
                    throw ApiException.invalidParameter("departureFrom",
                            "початок діапазону не може бути пізнішим за departureTo");
                }
                Set<Flight> inRange = new HashSet<>(repository.findByDepartureBetween(from, to));
                result = result.stream().filter(inRange::contains).toList();
            }
            json.write(response, 200, query.finish(repository, result, comparators, "flightNumber"));
        }
    }
}
