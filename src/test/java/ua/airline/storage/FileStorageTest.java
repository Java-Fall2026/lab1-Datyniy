package ua.airline.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

    @Test
    void savesAndLoadsPassengerAsJsonWithIsoDate() throws IOException {
        FileStorage<Passenger> storage = new FileStorage<>(Passenger.class);
        Path file = tempDir.resolve("passengers.json");
        Passenger passenger = new Passenger("PP10001", "Іван Петренко", LocalDate.of(2000, 5, 12));

        storage.save(file, List.of(passenger));

        assertTrue(Files.readString(file).contains("\"2000-05-12\""));
        assertEquals(List.of(passenger), storage.load(file));
    }

    @Test
    void savesAndLoadsFlightAsYamlWithIsoDateTimes() throws IOException {
        FileStorage<Flight> storage = new FileStorage<>(Flight.class);
        Path file = tempDir.resolve("flights.yaml");
        LocalDateTime departure = LocalDateTime.of(2026, 11, 2, 10, 30, 45);
        Flight flight = new Flight("PS-101", new Airport("KBP", "Київ"),
                new Airport("WAW", "Варшава"), departure, departure.plusHours(2),
                FlightStatus.BOARDING);

        storage.save(file, List.of(flight));

        String yaml = Files.readString(file);
        assertTrue(yaml.contains("\"2026-11-02T10:30:45\""));
        assertTrue(yaml.contains("\"2026-11-02T12:30:45\""));
        List<Flight> flights = storage.load(file);
        assertEquals(1, flights.size());
        Flight restored = flights.getFirst();
        assertEquals(flight.getFlightNumber(), restored.getFlightNumber());
        assertEquals(flight.getDepartureAirport(), restored.getDepartureAirport());
        assertEquals(flight.getArrivalAirport(), restored.getArrivalAirport());
        assertEquals(flight.getDepartureTime(), restored.getDepartureTime());
        assertEquals(flight.getArrivalTime(), restored.getArrivalTime());
        assertEquals(flight.getStatus(), restored.getStatus());
    }

    @Test
    void rejectsUnknownExtensionOnLoadAndSave() {
        FileStorage<Passenger> storage = new FileStorage<>(Passenger.class);
        Path file = tempDir.resolve("passengers.txt");

        var readError = assertThrows(IllegalArgumentException.class, () -> storage.load(file));
        var writeError = assertThrows(IllegalArgumentException.class, () -> storage.save(file, List.of()));

        assertTrue(readError.getMessage().contains("passengers.txt"));
        assertTrue(writeError.getMessage().contains("passengers.txt"));
        assertFalse(Files.exists(file));
    }
}
