package ua.airline.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.airline.model.Passenger;
import ua.airline.util.AirlineUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PassengerRepository extends GenericRepository<Passenger> {

    private static final Logger log = LoggerFactory.getLogger(PassengerRepository.class);

    public PassengerRepository() {
        super("passengers", Passenger::passportSeriesNum);
    }

    public List<Passenger> findAllByName() {
        List<Passenger> result = findAllSorted(
                (left, right) -> {
                    int comparison = left.fullName().compareTo(right.fullName());
                    return comparison != 0 ? comparison : left.compareTo(right);
                });
        log.info("Sorted passengers by name, then natural order: {} items", result.size());
        return result;
    }

    public List<Passenger> findAllByBirthDate() {
        List<Passenger> result = findAllSorted(
                Comparator.comparing(Passenger::birthDate)
                        .thenComparing(Comparator.naturalOrder()));
        log.info("Sorted passengers by birth date, then natural order: {} items", result.size());
        return result;
    }

    public List<Passenger> findByNameContaining(String text) {
        String query = normalizeQuery(text);
        List<Passenger> result = stream()
                .filter(p -> p.fullName().toLowerCase(Locale.ROOT).contains(query))
                .sorted()
                .toList();
        logSearch("name contains '" + query + "'", result.size());
        return result;
    }

    /**
     * Includes passengers born exactly on either endpoint.
     */
    public List<Passenger> findByBirthDateBetween(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<Passenger> result = stream()
                .filter(p -> !p.birthDate().isBefore(from) && !p.birthDate().isAfter(to))
                .sorted(Comparator.comparing(Passenger::birthDate)
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
        logSearch("birth date in [" + from + ", " + to + "]", result.size());
        return result;
    }

    public List<Passenger> findByNameAndBirthDateBetween(
            String text, LocalDate from, LocalDate to) {
        String query = normalizeQuery(text);
        validateRange(from, to);
        List<Passenger> result = stream()
                .filter(p -> p.fullName().toLowerCase(Locale.ROOT).contains(query))
                .filter(p -> !p.birthDate().isBefore(from) && !p.birthDate().isAfter(to))
                .sorted()
                .toList();
        logSearch("name contains '" + query + "', birth date in [" + from + ", " + to + "]",
                result.size());
        return result;
    }

    public Optional<Passenger> findFirstByName(String name) {
        String query = normalizeQuery(name);
        Optional<Passenger> result = stream()
                .filter(p -> p.fullName().toLowerCase(Locale.ROOT).equals(query))
                .sorted()
                .findFirst();
        logSearch("first exact name='" + query + "'", result.isEmpty() ? 0 : 1);
        return result;
    }

    public List<String> findDistinctNames(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Name limit must not be negative");
        }
        List<String> result = stream()
                .map(Passenger::fullName)
                .distinct()
                .sorted()
                .limit(limit)
                .toList();
        logSearch("distinct names ASC, limit=" + limit, result.size());
        return result;
    }

    private static String normalizeQuery(String text) {
        return AirlineUtils.requireNotBlank(text, "Passenger name query")
                .strip().toLowerCase(Locale.ROOT);
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        AirlineUtils.requireNotNull(from, "Range start");
        AirlineUtils.requireNotNull(to, "Range end");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Range start must not be after range end");
        }
    }

    private static void logSearch(String criterion, int count) {
        if (count == 0) {
            log.warn("Passenger search [{}]: 0 items", criterion);
        } else {
            log.info("Passenger search [{}]: {} items", criterion, count);
        }
    }
}
