package ua.airline.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.Passenger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageTest {
    @TempDir
    Path tempDir;

    @ParameterizedTest(name = "Passenger list round trip: {0}")
    @DisplayName("Preserves Passenger List On Save And Load")
    @ValueSource(strings = {"json", "yaml", "yml"})
    void preservesPassengerListOnSaveAndLoad(String extension) throws IOException {
        FileStorage<Passenger> storage = new FileStorage<>(Passenger.class);
        Path file = tempDir.resolve("passengers." + extension);
        List<Passenger> expected = List.of(
                new Passenger("PP10001", "Іван Петренко", LocalDate.of(2000, 5, 12)),
                new Passenger("PP10002", "Олена Коваленко", LocalDate.of(1998, 11, 3)));

        storage.save(file, expected);

        List<Passenger> actual = storage.load(file);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "ISO birth date: {0}")
    @DisplayName("Writes Birth Date As Iso String")
    @ValueSource(strings = {"json", "yaml"})
    void writesBirthDateAsIsoString(String extension) throws IOException {
        Path file = tempDir.resolve("passengers." + extension);
        new FileStorage<>(Passenger.class).save(file, List.of(
                new Passenger("PP10001", "Іван Петренко", LocalDate.of(2000, 5, 12))));

        assertTrue(Files.readString(file).contains("\"2000-05-12\""));
    }

    @Test
    @DisplayName("Preserves Flight List On Yaml Save And Load")
    void preservesFlightListOnYamlSaveAndLoad() throws IOException {
        FileStorage<Flight> storage = new FileStorage<>(Flight.class);
        Path file = tempDir.resolve("flights.yaml");
        List<Flight> expected = List.of(flight("PS-101"), flight("PS-102"));

        storage.save(file, expected);

        List<Flight> actual = storage.load(file);
        assertEquals(expected, actual);
        // Flight equality uses identity, so also compare all persisted values as lists.
        assertEquals(expected.stream().map(FlightData::of).toList(),
                actual.stream().map(FlightData::of).toList());
    }

    @ParameterizedTest(name = "ISO flight timestamp: {0}")
    @DisplayName("Writes Flight Times As Iso Strings")
    @ValueSource(strings = {"2026-11-02T10:30:45", "2026-11-02T12:30:45"})
    void writesFlightTimesAsIsoStrings(String expectedTime) throws IOException {
        Path file = tempDir.resolve("flights.yaml");
        new FileStorage<>(Flight.class).save(file, List.of(flight("PS-101")));

        assertTrue(Files.readString(file).contains("\"" + expectedTime + "\""));
    }

    @Test
    @DisplayName("Rejects Unknown Extension On Load")
    void rejectsUnknownExtensionOnLoad() {
        FileStorage<Passenger> storage = new FileStorage<>(Passenger.class);
        Path file = tempDir.resolve("passengers.txt");

        var error = assertThrows(IllegalArgumentException.class, () -> storage.load(file));

        assertTrue(error.getMessage().contains("passengers.txt"));
    }

    @Test
    @DisplayName("Rejects Unknown Extension On Save Without Creating File")
    void rejectsUnknownExtensionOnSaveWithoutCreatingFile() {
        FileStorage<Passenger> storage = new FileStorage<>(Passenger.class);
        Path file = tempDir.resolve("passengers.txt");

        var error = assertThrows(IllegalArgumentException.class, () -> storage.save(file, List.of()));

        assertTrue(error.getMessage().contains("passengers.txt"));
        assertFalse(Files.exists(file));
    }

    private static Flight flight(String number) {
        LocalDateTime departure = LocalDateTime.of(2026, 11, 2, 10, 30, 45);
        return new Flight(number, new Airport("KBP", "Київ"), new Airport("WAW", "Варшава"),
                departure, departure.plusHours(2), FlightStatus.BOARDING);
    }

    private record FlightData(String number, Airport departureAirport, Airport arrivalAirport,
                              LocalDateTime departureTime, LocalDateTime arrivalTime, FlightStatus status) {
        static FlightData of(Flight flight) {
            return new FlightData(flight.getFlightNumber(), flight.getDepartureAirport(),
                    flight.getArrivalAirport(), flight.getDepartureTime(), flight.getArrivalTime(),
                    flight.getStatus());
        }
    }
}
