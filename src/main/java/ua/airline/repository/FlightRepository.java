package ua.airline.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.util.AirlineUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FlightRepository extends GenericRepository<Flight> {

    private static final Logger log = LoggerFactory.getLogger(FlightRepository.class);

    public FlightRepository() {
        super("flights", Flight::getFlightNumber);
    }

    public List<Flight> findAllByDepartureTime() {
        List<Flight> result = findAllSorted(
                Comparator.comparing(Flight::getDepartureTime)
                        .thenComparing(Flight::getFlightNumber));
        log.info("Sorted flights by departure time, then number: {} items", result.size());
        return result;
    }

    public List<Flight> findAllByStatusAndLatestDeparture() {
        List<Flight> result = findAllSorted(
                Comparator.comparing(Flight::getStatus)
                        .thenComparing(Comparator.comparing(Flight::getDepartureTime).reversed())
                        .thenComparing(Flight::getFlightNumber));
        log.info("Sorted flights by status ASC, departure time DESC, number ASC: {} items",
                result.size());
        return result;
    }

    public List<Flight> findByNumberContaining(String text) {
        String query = AirlineUtils.requireNotBlank(text, "Flight number query")
                .strip().toLowerCase(Locale.ROOT);
        List<Flight> result = stream()
                .filter(f -> f.getFlightNumber().toLowerCase(Locale.ROOT).contains(query))
                .sorted()
                .toList();
        logSearch("number contains '" + query + "'", result.size());
        return result;
    }

    /**
     * Includes flights departing exactly at either endpoint.
     */
    public List<Flight> findByDepartureBetween(LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        List<Flight> result = stream()
                .filter(f -> !f.getDepartureTime().isBefore(from)
                        && !f.getDepartureTime().isAfter(to))
                .sorted(Comparator.comparing(Flight::getDepartureTime)
                        .thenComparing(Flight::getFlightNumber))
                .toList();
        logSearch("departure in [" + from + ", " + to + "]", result.size());
        return result;
    }

    public List<Flight> findByStatusAndDepartureBetween(
            FlightStatus status, LocalDateTime from, LocalDateTime to) {
        AirlineUtils.requireNotNull(status, "Flight status");
        validateRange(from, to);
        List<Flight> result = stream()
                .filter(f -> f.getStatus() == status)
                .filter(f -> !f.getDepartureTime().isBefore(from)
                        && !f.getDepartureTime().isAfter(to))
                .sorted(Comparator.comparing(Flight::getDepartureTime)
                        .thenComparing(Flight::getFlightNumber))
                .toList();
        logSearch("status=" + status + ", departure in [" + from + ", " + to + "]",
                result.size());
        return result;
    }

    public Optional<Flight> findFirstByStatus(FlightStatus status) {
        AirlineUtils.requireNotNull(status, "Flight status");
        Optional<Flight> result = stream()
                .filter(f -> f.getStatus() == status)
                .sorted(Comparator.comparing(Flight::getDepartureTime)
                        .thenComparing(Flight::getFlightNumber))
                .findFirst();
        logSearch("first departure with status=" + status, result.isEmpty() ? 0 : 1);
        return result;
    }

    public Map<FlightStatus, List<Flight>> groupByStatus() {
        Map<FlightStatus, List<Flight>> result = stream()
                .collect(Collectors.groupingBy(Flight::getStatus));
        log.info("Grouped {} flights by status into {} groups", size(), result.size());
        return result;
    }

    public Map<FlightStatus, Long> countByStatus() {
        Map<FlightStatus, Long> result = stream()
                .collect(Collectors.groupingBy(Flight::getStatus, Collectors.counting()));
        log.info("Counted {} flights by status into {} groups", size(), result.size());
        return result;
    }

    public long totalDurationMinutes() {
        long result = stream().mapToLong(AirlineUtils::flightDurationMinutes).sum();
        log.info("Total duration of {} flights: {} minutes", size(), result);
        return result;
    }

    private static void validateRange(LocalDateTime from, LocalDateTime to) {
        AirlineUtils.requireNotNull(from, "Range start");
        AirlineUtils.requireNotNull(to, "Range end");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Range start must not be after range end");
        }
    }

    private static void logSearch(String criterion, int count) {
        if (count == 0) {
            log.warn("Flight search [{}]: 0 items", criterion);
        } else {
            log.info("Flight search [{}]: {} items", criterion, count);
        }
    }
}
