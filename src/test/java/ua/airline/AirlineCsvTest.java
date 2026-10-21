package ua.airline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ua.airline.model.Airport;
import ua.airline.model.Passenger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AirlineCsvTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesPassengerAndTrimsFields() throws InvalidDataException {
        Passenger passenger = AirlineCsvParsers.parsePassenger(
                " PP10001 , Ivan Petrenko , 2000-05-12 ", 2
        );

        assertEquals(
                Passenger.of("PP10001", "Ivan Petrenko", LocalDate.of(2000, 5, 12)),
                passenger
        );
    }

    @Test
    void parsesAirportAndNormalizesCode() throws InvalidDataException {
        Airport airport = AirlineCsvParsers.parseAirport(" kbp , Kyiv ", 2);

        assertEquals(Airport.of("KBP", "Kyiv"), airport);
    }

    @Test
    void wrapsInvalidDateWithLineNumberAndOriginalCause() {
        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> AirlineCsvParsers.parsePassenger("PP1,Ivan,not-a-date", 7)
        );

        assertTrue(exception.getMessage().startsWith("Line 7:"));
        assertInstanceOf(DateTimeParseException.class, exception.getCause());
    }

    @Test
    void wrapsEmptyNameValidationWithLineNumberAndCause() {
        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> AirlineCsvParsers.parsePassenger("PP1,,2000-05-12", 8)
        );

        assertTrue(exception.getMessage().startsWith("Line 8:"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Full name"));
    }

    @Test
    void rejectsFutureBirthDate() {
        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> AirlineCsvParsers.parsePassenger(
                        "PP1,Ivan," + LocalDate.now().plusDays(1), 9
                )
        );

        assertTrue(exception.getMessage().startsWith("Line 9:"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    void rejectsMissingColumn() {
        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> AirlineCsvParsers.parsePassenger("PP1,Ivan", 10)
        );

        assertTrue(exception.getMessage().startsWith("Line 10:"));
        assertTrue(exception.getMessage().contains("expected 3 columns, got 2"));
    }

    @Test
    void rejectsExtraColumn() {
        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> AirlineCsvParsers.parseAirport("CDG,Paris,Extra", 11)
        );

        assertTrue(exception.getMessage().startsWith("Line 11:"));
        assertTrue(exception.getMessage().contains("expected 2 columns, got 3"));
    }

    @Test
    void wrapsInvalidAirportCode() {
        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> AirlineCsvParsers.parseAirport("12A,Kyiv", 12)
        );

        assertTrue(exception.getMessage().startsWith("Line 12:"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    void preservesTrailingEmptyFieldForValidation() {
        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> AirlineCsvParsers.parseAirport("LHR,", 13)
        );

        assertTrue(exception.getMessage().startsWith("Line 13:"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("City name"));
    }

    @Test
    void rejectsBlankRow() {
        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> AirlineCsvParsers.parsePassenger("", 14)
        );

        assertTrue(exception.getMessage().startsWith("Line 14:"));
    }

    @Test
    void continuesAfterDifferentInvalidRowsAndUsesPhysicalLineNumbers() throws IOException {
        Path file = writeCsv(
                "passengers.csv",
                AirlineCsvParsers.PASSENGER_HEADER + "\n"
                        + "PP1,Іван Петренко,2000-05-12\n"
                        + "PP2,Bad Date,not-a-date\n"
                        + "PP3,,2000-05-12\n"
                        + "PP4,Missing Date\n"
                        + "PP5,Олена Коваленко,1998-11-03\n"
        );
        List<Integer> lineNumbers = new ArrayList<>();
        CsvLoader<Passenger> loader = new CsvLoader<>(
                AirlineCsvParsers.PASSENGER_HEADER,
                (line, number) -> {
                    lineNumbers.add(number);
                    return AirlineCsvParsers.parsePassenger(line, number);
                }
        );

        List<Passenger> passengers = loader.load(file);

        assertEquals(List.of(2, 3, 4, 5, 6), lineNumbers);
        assertEquals(2, passengers.size());
        assertEquals("Іван Петренко", passengers.get(0).fullName());
        assertEquals("PP5", passengers.get(1).passportSeriesNum());
        assertEquals("Олена Коваленко", passengers.get(1).fullName());
    }

    @Test
    void loadsAirportsAfterInvalidRows() throws IOException {
        Path file = writeCsv(
                "airports.csv",
                AirlineCsvParsers.AIRPORT_HEADER + "\n"
                        + "KBP,Kyiv\n"
                        + "12A,Invalid Code\n"
                        + "LHR,\n"
                        + "CDG,Paris,Extra\n"
                        + "WAW,Warsaw\n"
        );
        CsvLoader<Airport> loader = new CsvLoader<>(
                AirlineCsvParsers.AIRPORT_HEADER, AirlineCsvParsers::parseAirport
        );

        assertEquals(
                List.of(Airport.of("KBP", "Kyiv"), Airport.of("WAW", "Warsaw")),
                loader.load(file)
        );
    }

    @Test
    void missingFileReturnsEmptyListWithoutThrowing() {
        List<Passenger> passengers = assertDoesNotThrow(
                () -> passengerLoader().load(tempDir.resolve("missing.csv"))
        );

        assertTrue(passengers.isEmpty());
    }

    @Test
    void rejectsWrongHeaderWithoutTreatingDataAsPassengers() throws IOException {
        Path file = writeCsv(
                "wrong-header.csv",
                "wrong,header,columns\nPP1,Ivan,2000-05-12\n"
        );

        assertTrue(passengerLoader().load(file).isEmpty());
    }

    @Test
    void emptyFileReturnsEmptyListWithoutThrowing() throws IOException {
        Path file = writeCsv("empty.csv", "");

        List<Passenger> passengers = assertDoesNotThrow(
                () -> passengerLoader().load(file)
        );

        assertTrue(passengers.isEmpty());
    }

    private CsvLoader<Passenger> passengerLoader() {
        return new CsvLoader<>(
                AirlineCsvParsers.PASSENGER_HEADER, AirlineCsvParsers::parsePassenger
        );
    }

    private Path writeCsv(String name, String content) throws IOException {
        return Files.writeString(tempDir.resolve(name), content, StandardCharsets.UTF_8);
    }
}
