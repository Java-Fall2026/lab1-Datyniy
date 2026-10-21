package ua.airline.util;

import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.SeatClass;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AirlineUtils {

    private static final String IATA_PATTERN = "[A-Z]{3}";

    private AirlineUtils() {
    }

    public static <T> T requireNotNull(T value, String fieldName) {
        return ValidationHelper.requireNotNull(value, fieldName);
    }

    public static String requireNotBlank(String value, String fieldName) {
        return ValidationHelper.requireNotBlank(value, fieldName);
    }

    public static double requirePositive(double value, String fieldName) {
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
        requireNotBlank(value, "Value");
        return FormatHelper.normalize(value);
    }

    public static String normalizeLowercase(String value) {
        requireNotBlank(value, "Value");
        return FormatHelper.normalizeLowercase(value);
    }

    public static String capitalize(String value) {
        requireNotBlank(value, "Value");
        return FormatHelper.capitalize(value);
    }

    public static String formatMoney(double value) {
        return FormatHelper.formatMoney(value);
    }

    public static LocalDate validateBirthDate(LocalDate birthDate) {
        requireNotNull(birthDate, "Birth date");

        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Birth date must not be in the future, got: "
                            + birthDate
            );
        }

        return birthDate;
    }

    public static String validateIataCode(String iataCode) {
        requireNotBlank(iataCode, "IATA code");

        String normalized = FormatHelper.normalize(iataCode);

        if (!normalized.matches(IATA_PATTERN)) {
            throw new IllegalArgumentException(
                    "IATA code must contain exactly 3 Latin letters, got: "
                            + iataCode
            );
        }

        return normalized;
    }

    public static void requireDifferentAirports(
            Airport departureAirport,
            Airport arrivalAirport
    ) {
        requireNotNull(departureAirport, "Departure airport");
        requireNotNull(arrivalAirport, "Arrival airport");

        if (departureAirport.iataCode().equals(arrivalAirport.iataCode())) {
            throw new IllegalArgumentException(
                    "Arrival airport must differ from departure airport, got: "
                            + arrivalAirport.iataCode()
            );
        }
    }

    public static void requireArrivalAfterDeparture(
            LocalDateTime departureTime,
            LocalDateTime arrivalTime
    ) {
        requireNotNull(departureTime, "Departure time");
        requireNotNull(arrivalTime, "Arrival time");

        if (!arrivalTime.isAfter(departureTime)) {
            throw new IllegalArgumentException(
                    "Arrival time must be after departure time, got: "
                            + arrivalTime
            );
        }
    }

    public static long flightDurationMinutes(Flight flight) {
        requireNotNull(flight, "Flight");

        return Duration.between(
                flight.getDepartureTime(),
                flight.getArrivalTime()
        ).toMinutes();
    }

    public static String route(Flight flight) {
        requireNotNull(flight, "Flight");

        return flight.getDepartureAirport().iataCode()
                + " → "
                + flight.getArrivalAirport().iataCode();
    }

    public static String statusDescription(FlightStatus status) {
        requireNotNull(status, "Flight status");

        return switch (status) {
            case SCHEDULED -> "Рейс заплановано";
            case BOARDING -> "Триває посадка";
            case DELAYED -> "Рейс затримано";
            case COMPLETED -> "Рейс завершено";
            case CANCELLED -> "Рейс скасовано";
        };
    }

    public static String serviceDescription(SeatClass seatClass) {
        requireNotNull(seatClass, "Seat class");

        return switch (seatClass) {
            case ECONOMY -> "Стандартне обслуговування";
            case BUSINESS -> "Підвищений комфорт";
            case FIRST -> "Обслуговування першого класу";
        };
    }
}