package ua.airline.util;

import ua.airline.model.Airport;
import ua.airline.model.Flight;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public final class AirlineUtils {

    static final String IATA_PATTERN = "[A-Z]{3}";

    static final Set<String> ALLOWED_STATUSES = Set.of(
            "SCHEDULED",
            "BOARDING",
            "DELAYED",
            "COMPLETED",
            "CANCELLED"
    );

    static final Set<String> ALLOWED_SEAT_CLASSES = Set.of(
            "ECONOMY",
            "BUSINESS",
            "FIRST"
    );

    private AirlineUtils() {
    }

    public static <T> T requireNotNull(
            T value,
            String fieldName
    ) {
        return ValidationHelper.requireNotNull(value, fieldName);
    }

    public static String requireNotBlank(
            String value,
            String fieldName
    ) {
        return ValidationHelper.requireNotBlank(value, fieldName);
    }

    public static double requirePositive(
            double value,
            String fieldName
    ) {
        return ValidationHelper.requirePositive(value, fieldName);
    }

    public static int requireInRange(
            int value,
            int min,
            int max,
            String fieldName
    ) {
        return ValidationHelper.requireInRange(
                value,
                min,
                max,
                fieldName
        );
    }

    public static String normalize(String value) {
        ValidationHelper.requireNotBlank(value, "Value");
        return FormatHelper.normalize(value);
    }

    public static String normalizeLowercase(String value) {
        ValidationHelper.requireNotBlank(value, "Value");
        return FormatHelper.normalizeLowercase(value);
    }

    public static String capitalize(String value) {
        ValidationHelper.requireNotBlank(value, "Value");
        return FormatHelper.capitalize(value);
    }

    public static String formatMoney(double value) {
        return FormatHelper.formatMoney(value);
    }

    public static LocalDate validateBirthDate(LocalDate birthDate) {
        ValidationHelper.requireNotNull(birthDate, "Birth date");

        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Birth date must not be in the future, got: "
                            + birthDate
            );
        }

        return birthDate;
    }

    public static String validateIataCode(String iataCode) {
        ValidationHelper.requireNotBlank(iataCode, "IATA code");

        String normalized = FormatHelper.normalize(iataCode);

        if (!normalized.matches(IATA_PATTERN)) {
            throw new IllegalArgumentException(
                    "IATA code must contain exactly 3 Latin letters, got: "
                            + iataCode
            );
        }

        return normalized;
    }

    public static String validateStatus(String status) {
        ValidationHelper.requireNotBlank(status, "Flight status");

        String normalized = FormatHelper.normalize(status);

        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Flight status must be one of "
                            + ALLOWED_STATUSES
                            + ", got: " + status
            );
        }

        return normalized;
    }

    public static String validateSeatClass(String seatClass) {
        ValidationHelper.requireNotBlank(seatClass, "Seat class");

        String normalized = FormatHelper.normalize(seatClass);

        if (!ALLOWED_SEAT_CLASSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Seat class must be one of "
                            + ALLOWED_SEAT_CLASSES
                            + ", got: " + seatClass
            );
        }

        return normalized;
    }

    public static void requireDifferentAirports(
            Airport departureAirport,
            Airport arrivalAirport
    ) {
        ValidationHelper.requireNotNull(
                departureAirport,
                "Departure airport"
        );
        ValidationHelper.requireNotNull(
                arrivalAirport,
                "Arrival airport"
        );

        if (departureAirport.equals(arrivalAirport)) {
            throw new IllegalArgumentException(
                    "Arrival airport must differ from departure airport, got: "
                            + arrivalAirport.getIataCode()
            );
        }
    }

    public static void requireArrivalAfterDeparture(
            LocalDateTime departureTime,
            LocalDateTime arrivalTime
    ) {
        ValidationHelper.requireNotNull(
                departureTime,
                "Departure time"
        );
        ValidationHelper.requireNotNull(
                arrivalTime,
                "Arrival time"
        );

        if (!arrivalTime.isAfter(departureTime)) {
            throw new IllegalArgumentException(
                    "Arrival time must be after departure time, got: "
                            + arrivalTime
            );
        }
    }

    public static long flightDurationMinutes(Flight flight) {
        ValidationHelper.requireNotNull(flight, "Flight");

        return Duration.between(
                flight.getDepartureTime(),
                flight.getArrivalTime()
        ).toMinutes();
    }

    public static String route(Flight flight) {
        ValidationHelper.requireNotNull(flight, "Flight");

        return flight.getDepartureAirport().getIataCode()
                + " → "
                + flight.getArrivalAirport().getIataCode();
    }
}