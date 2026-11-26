package ua.airline.api;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.airline.model.Passenger;
import ua.airline.repository.PassengerRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PassengerServlet extends HttpServlet {
    private final PassengerRepository repository;
    private final JsonSupport json;
    private final Map<String, Comparator<Passenger>> comparators = Map.of(
            "passportSeriesNum", Comparator.naturalOrder(),
            "fullName", Comparator.comparing(Passenger::fullName).thenComparing(Passenger::passportSeriesNum),
            "birthDate", Comparator.comparing(Passenger::birthDate).thenComparing(Passenger::passportSeriesNum));

    public PassengerServlet(PassengerRepository repository, JsonSupport json) {
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
                Passenger passenger = json.readValid(request, Passenger.class);
                String identity = repository.identityOf(passenger);
                checkIdentity(identity);
                repository.add(passenger);
                response.setHeader("Location", ApiPath.location(request, identity));
                json.write(response, 201, passenger);
            }
            case "PUT" -> {
                String identity = path.requireIdentity();
                Passenger passenger = json.readValid(request, Passenger.class);
                if (!identity.equals(repository.identityOf(passenger))) {
                    throw new ApiException(400, "Body identity does not match path",
                            "Ідентичність у тілі не збігається зі шляхом");
                }
                if (!repository.replace(passenger)) {
                    throw ApiException.notFound();
                }
                json.write(response, 200, passenger);
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
            json.write(response, 200, Map.of("byBirthYear", repository.countByBirthYear()));
        } else if (!path.collection()) {
            json.write(response, 200, repository.findByIdentity(path.identity()).orElseThrow(ApiException::notFound));
        } else {
            QueryParameters query = new QueryParameters(request,
                    Set.of("name", "birthFrom", "birthTo", "sort", "order", "limit", "offset"));
            String name = query.text("name");
            boolean range = query.hasRange("birthFrom", "birthTo");
            LocalDate from = query.date("birthFrom", LocalDate.MIN);
            LocalDate to = query.date("birthTo", LocalDate.MAX);
            if (from.isAfter(to)) {
                throw ApiException.invalidParameter("birthFrom", "початок діапазону не може бути пізнішим за birthTo");
            }
            List<Passenger> result;
            if (name != null && range) {
                result = repository.findByNameAndBirthDateBetween(name, from, to);
            } else if (name != null) {
                result = repository.findByNameContaining(name);
            } else if (range) {
                result = repository.findByBirthDateBetween(from, to);
            } else {
                result = repository.findAll();
            }
            json.write(response, 200, query.finish(repository, result, comparators, "passportSeriesNum"));
        }
    }

    private static void checkIdentity(String identity) {
        if (identity.equals("stats")) {
            throw new ApiException(400, "Identity stats is reserved", "Ідентичність stats зарезервована для статистики");
        }
    }
}
