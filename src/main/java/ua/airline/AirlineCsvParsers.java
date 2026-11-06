package ua.airline;

import ua.airline.model.Airport;
import ua.airline.model.Passenger;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class AirlineCsvParsers {

    public static final String PASSENGER_HEADER = "passportSeriesNum,fullName,birthDate";
    public static final String AIRPORT_HEADER = "iataCode,cityName";

    private AirlineCsvParsers() {
    }

    public static Passenger parsePassenger(String line, int lineNumber)
            throws InvalidDataException {

        String[] fields = line.split(",", -1);
        if (fields.length != 3) {
            throw new InvalidDataException(
                    "Line " + lineNumber + ": expected 3 columns, got " + fields.length
            );
        }

        try {
            return Passenger.of(
                    fields[0].trim(),
                    fields[1].trim(),
                    LocalDate.parse(fields[2].trim())
            );
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new InvalidDataException(
                    "Line " + lineNumber + ": invalid passenger data: " + e.getMessage(),
                    e
            );
        }
    }

    public static Airport parseAirport(String line, int lineNumber)
            throws InvalidDataException {

        String[] fields = line.split(",", -1);
        if (fields.length != 2) {
            throw new InvalidDataException(
                    "Line " + lineNumber + ": expected 2 columns, got " + fields.length
            );
        }

        try {
            return Airport.of(fields[0].trim(), fields[1].trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException(
                    "Line " + lineNumber + ": invalid airport data: " + e.getMessage(),
                    e
            );
        }
    }
}
