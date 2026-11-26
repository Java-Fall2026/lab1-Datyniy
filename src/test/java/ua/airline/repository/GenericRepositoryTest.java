package ua.airline.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Finds Added Item")
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
    @DisplayName("Returns Empty Optional For Missing Identity")
    void returnsEmptyOptionalForMissingIdentity() {
        assertTrue(repository.findByIdentity("ZZZ").isEmpty());
    }

    @Test
    @DisplayName("Rejects Duplicate Identity Without Replacing Original")
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
    @DisplayName("Add If Absent Returns False Without Changing Size")
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
    @DisplayName("Removes Existing Item")
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
    @DisplayName("Returns False When Removing Missing Item")
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
    @DisplayName("Find All Returns Unmodifiable List")
    void findAllReturnsUnmodifiableList() {
        repository.add(Airport.of("KBP", "Kyiv"));
        List<Airport> result = repository.findAll();

        assertThrows(UnsupportedOperationException.class, result::clear);
        assertEquals(result, repository.findAll());
    }

    @Test
    @DisplayName("Find All Returns Snapshot Unaffected By Later Additions")
    void findAllReturnsSnapshotUnaffectedByLaterAdditions() {
        Airport first = Airport.of("KBP", "Kyiv");
        Airport second = Airport.of("WAW", "Warsaw");
        repository.add(first);
        List<Airport> snapshot = repository.findAll();

        repository.add(second);

        assertEquals(List.of(first), snapshot);
        assertEquals(List.of(first, second), repository.findAll());
    }


    @Test
    @DisplayName("Works With Different Domain Types")
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
    @DisplayName("Rejects Null Item Without Changing Repository")
    void rejectsNullItemWithoutChangingRepository() {
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.add(null)
        );
        assertEquals(0, repository.size());
    }

    @ParameterizedTest
    @DisplayName("Rejects Missing Or Blank Extracted Identity")
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
    @DisplayName("Adds All Items In Collection Order")
    void addsAllItemsInCollectionOrder() {
        Airport first = Airport.of("KBP", "Kyiv");
        Airport second = Airport.of("WAW", "Warsaw");

        repository.addAll(List.of(first, second));

        assertEquals(List.of(first, second), repository.findAll());
        assertEquals(2, repository.size());
    }

    @Test
    @DisplayName("Add All Stops At Duplicate And Keeps Earlier Additions")
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
    @DisplayName("Copies All Items Without Removing Them From Source")
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
    @DisplayName("Keeps Flight Searchable After Changing Its Status")
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
    @Test
    @DisplayName("Replace updates an existing identity and preserves insertion order")
    void replacesExistingIdentity() {
        repository.add(Airport.of("KBP", "Kyiv"));
        Airport second = Airport.of("WAW", "Warsaw");
        repository.add(second);
        Airport changed = Airport.of("KBP", "Updated city");

        assertTrue(repository.replace(changed));
        assertEquals(List.of(changed, second), repository.findAll());
        assertSame(changed, repository.findByIdentity("KBP").orElseThrow());
    }

    @Test
    @DisplayName("Replace of a missing identity does not insert an object")
    void doesNotInsertDuringReplace() {
        assertFalse(repository.replace(Airport.of("KBP", "Kyiv")));
        assertEquals(0, repository.size());
    }

    @Test
    @DisplayName("IdentityOf uses the configured extractor")
    void exposesExtractedIdentity() {
        assertEquals("KBP", repository.identityOf(Airport.of("KBP", "Kyiv")));
    }

}
