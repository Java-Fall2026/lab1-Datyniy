package ua.airline.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.Passenger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenericRepositoryTest {

    private GenericRepository<Airport> repository;

    @BeforeEach
    void setUp() {
        repository = new GenericRepository<>(
                "airports",
                airport -> airport.iataCode()
        );
    }

    @Test
    void findsAddedItem() {
        Airport airport = Airport.of("KBP", "Kyiv");

        repository.add(airport);

        assertSame(
                airport,
                repository.findByIdentity("KBP").orElseThrow()
        );
        assertEquals(1, repository.size());
    }

    @Test
    void returnsEmptyOptionalForMissingIdentity() {
        assertTrue(repository.findByIdentity("ZZZ").isEmpty());
    }

    @Test
    void rejectsDuplicateIdentityWithoutReplacingOriginal() {
        Airport original = Airport.of("KBP", "Kyiv");
        repository.add(original);

        DuplicateIdentityException exception = assertThrows(
                DuplicateIdentityException.class,
                () -> repository.add(
                        Airport.of("KBP", "Another city")
                )
        );

        assertTrue(exception.getMessage().contains("KBP"));
        assertTrue(exception.getMessage().contains("airports"));
        assertEquals(1, repository.size());
        assertSame(
                original,
                repository.findByIdentity("KBP").orElseThrow()
        );
    }

    @Test
    void addIfAbsentReturnsFalseWithoutChangingSize() {
        Airport airport = Airport.of("KBP", "Kyiv");
        assertTrue(repository.addIfAbsent(airport));
        int sizeBeforeDuplicate = repository.size();

        boolean added = repository.addIfAbsent(airport);

        assertFalse(added);
        assertEquals(sizeBeforeDuplicate, repository.size());
        assertSame(
                airport,
                repository.findByIdentity("KBP").orElseThrow()
        );
    }

    @Test
    void removesExistingItem() {
        repository.add(Airport.of("KBP", "Kyiv"));
        Airport remaining = Airport.of("WAW", "Warsaw");
        repository.add(remaining);

        assertTrue(repository.removeByIdentity("KBP"));
        assertEquals(1, repository.size());
        assertTrue(repository.findByIdentity("KBP").isEmpty());
        assertSame(
                remaining,
                repository.findByIdentity("WAW").orElseThrow()
        );
    }

    @Test
    void returnsFalseWhenRemovingMissingItem() {
        Airport airport = Airport.of("KBP", "Kyiv");
        repository.add(airport);

        assertFalse(repository.removeByIdentity("ZZZ"));
        assertEquals(1, repository.size());
        assertSame(
                airport,
                repository.findByIdentity("KBP").orElseThrow()
        );
    }

    @Test
    void findAllReturnsUnmodifiableSnapshot() {
        Airport airport = Airport.of("KBP", "Kyiv");
        repository.add(airport);
        List<Airport> result = repository.findAll();

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.clear()
        );
        assertEquals(1, repository.size());
        assertSame(
                airport,
                repository.findByIdentity("KBP").orElseThrow()
        );

        Airport nextAirport = Airport.of("WAW", "Warsaw");
        repository.add(nextAirport);

        assertEquals(List.of(airport), result);
        assertEquals(List.of(airport, nextAirport), repository.findAll());
    }

    @Test
    void worksWithDifferentDomainTypes() {
        GenericRepository<Passenger> passengers =
                new GenericRepository<>(
                        "passengers",
                        passenger -> passenger.passportSeriesNum()
                );

        Airport airport = Airport.of("KBP", "Kyiv");
        Passenger passenger = Passenger.of(
                "PP10001",
                "Daria Petrova",
                LocalDate.of(2000, 5, 12)
        );

        repository.add(airport);
        passengers.add(passenger);

        assertSame(
                airport,
                repository.findByIdentity("KBP").orElseThrow()
        );
        assertSame(
                passenger,
                passengers.findByIdentity("PP10001").orElseThrow()
        );
        assertEquals(1, repository.size());
        assertEquals(1, passengers.size());
    }

    @Test
    void rejectsNullItemWithoutChangingRepository() {
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.add(null)
        );
        assertEquals(0, repository.size());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingOrBlankExtractedIdentity(String identity) {
        GenericRepository<Airport> invalidRepository = new GenericRepository<>(
                "invalid-airports",
                airport -> identity
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> invalidRepository.add(Airport.of("KBP", "Kyiv"))
        );
        assertEquals(0, invalidRepository.size());
    }

    @Test
    void addsAllItemsInCollectionOrder() {
        Airport first = Airport.of("KBP", "Kyiv");
        Airport second = Airport.of("WAW", "Warsaw");

        repository.addAll(List.of(first, second));

        assertEquals(List.of(first, second), repository.findAll());
        assertEquals(2, repository.size());
    }

    @Test
    void addAllStopsAtDuplicateAndKeepsEarlierAdditions() {
        Airport original = Airport.of("KBP", "Kyiv");
        Airport beforeDuplicate = Airport.of("BER", "Berlin");
        repository.add(original);

        assertThrows(
                DuplicateIdentityException.class,
                () -> repository.addAll(List.of(
                        beforeDuplicate,
                        Airport.of("KBP", "Another city"),
                        Airport.of("WAW", "Warsaw")
                ))
        );

        assertEquals(List.of(original, beforeDuplicate), repository.findAll());
        assertTrue(repository.findByIdentity("WAW").isEmpty());
    }

    @Test
    void copiesAllItemsWithoutRemovingThemFromSource() {
        Airport first = Airport.of("KBP", "Kyiv");
        Airport second = Airport.of("WAW", "Warsaw");
        repository.addAll(List.of(first, second));
        GenericRepository<Airport> backup = new GenericRepository<>(
                "airport-backup",
                airport -> airport.iataCode()
        );

        GenericRepository.copyAll(repository, backup);

        assertEquals(repository.findAll(), backup.findAll());
        assertEquals(2, repository.size());
        assertSame(first, backup.findByIdentity("KBP").orElseThrow());
        assertSame(second, backup.findByIdentity("WAW").orElseThrow());
    }

    @Test
    void keepsFlightSearchableAfterChangingItsStatus() {
        GenericRepository<Flight> flights = new GenericRepository<>(
                "flights",
                flight -> flight.getFlightNumber()
        );
        Flight flight = new Flight(
                "PS-101",
                Airport.of("KBP", "Kyiv"),
                Airport.of("WAW", "Warsaw"),
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 11, 30),
                FlightStatus.SCHEDULED
        );
        flights.add(flight);

        flight.setStatus(FlightStatus.BOARDING);

        Flight found = flights.findByIdentity("PS-101").orElseThrow();
        assertSame(flight, found);
        assertEquals(FlightStatus.BOARDING, found.getStatus());
        assertEquals(1, flights.size());
    }
}
