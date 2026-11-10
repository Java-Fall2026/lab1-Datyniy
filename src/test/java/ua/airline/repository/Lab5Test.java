package ua.airline.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.FlightTicket;
import ua.airline.model.Passenger;
import ua.airline.model.SeatClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class Lab5Test {

    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final LocalDate BIRTH_FROM = LocalDate.of(1995, 1, 1);
    private static final LocalDate BIRTH_TO = LocalDate.of(2005, 12, 31);

    private FlightRepository flights;
    private PassengerRepository passengers;
    private Flight early;
    private Flight late;
    private Flight delayed;
    private Flight outside;
    private Passenger anna;
    private Passenger olderAnna;
    private Passenger bohdan;

    @BeforeEach
    void setUp() {
        early = flight("PS-100", START, FlightStatus.SCHEDULED);
        late = flight("PS-200", START.plusHours(3), FlightStatus.SCHEDULED);
        delayed = flight("LO-300", START.plusHours(1), FlightStatus.DELAYED);
        outside = flight("PS-400", START.plusDays(1), FlightStatus.SCHEDULED);
        flights = new FlightRepository();
        flights.addAll(List.of(outside, late, delayed, early));

        anna = Passenger.of("PP-200", "Anna Kovalenko", LocalDate.of(2000, 1, 1));
        olderAnna = Passenger.of("PP-100", "Anna Kovalenko", LocalDate.of(1990, 1, 1));
        bohdan = Passenger.of("PP-300", "Bohdan Melnyk", LocalDate.of(2003, 1, 1));
        passengers = new PassengerRepository();
        passengers.addAll(List.of(bohdan, anna, olderAnna));
    }

    @Test
    void collectionsSortUsesFlightNumbers() {
        List<Flight> result = new ArrayList<>(flights.findAll());
        Collections.sort(result);
        assertEquals(List.of(delayed, early, late, outside), result);
    }

    @Test
    void collectionsSortUsesPassengerNaturalOrder() {
        List<Passenger> result = new ArrayList<>(passengers.findAll());
        Collections.sort(result);
        assertEquals(List.of(olderAnna, anna, bohdan), result);
    }

    @Test
    void passengerNaturalOrderBreaksTiesUsingAllRecordFields() {
        Passenger first = Passenger.of("SAME", "Anna", LocalDate.of(2000, 1, 1));
        Passenger differentName = Passenger.of("SAME", "Bohdan", first.birthDate());
        Passenger differentBirthDate = Passenger.of("SAME", "Anna", LocalDate.of(2001, 1, 1));
        Passenger copy = Passenger.of("SAME", "Anna", first.birthDate());

        assertTrue(first.compareTo(differentName) < 0);
        assertTrue(first.compareTo(differentBirthDate) < 0);
        assertEquals(0, first.compareTo(copy));
        assertEquals(first, copy);
        assertNotEquals(first, differentName);
        assertNotEquals(first, differentBirthDate);
    }

    @Test
    void flightNaturalOrderMatchesIdentityBasedEquality() {
        Flight sameNumber = flight(early.getFlightNumber(), START.plusDays(5), FlightStatus.CANCELLED);
        assertEquals(early, sameNumber);
        assertEquals(0, early.compareTo(sameNumber));
    }

    @Test
    void multiLevelComparatorSortsStatusesAndReversesOnlyDepartureTime() {
        assertEquals(List.of(outside, late, early, delayed),
                flights.findAllByStatusAndLatestDeparture());
    }

    @Test
    void departureTimeSortUsesFlightNumberForTies() {
        Flight tied = flight("AA-001", START, FlightStatus.CANCELLED);
        flights.add(tied);
        assertEquals(List.of(tied, early, delayed, late, outside), flights.findAllByDepartureTime());
    }

    @Test
    void passengerNameSortBreaksTiesByNaturalOrder() {
        assertEquals(List.of(olderAnna, anna, bohdan), passengers.findAllByName());
    }

    @Test
    void passengerBirthDateSortOrdersOldestFirst() {
        assertEquals(List.of(olderAnna, anna, bohdan), passengers.findAllByBirthDate());
    }

    @Test
    void identityDescendingIsTheReverseOfAscendingForBothRepositories() {
        List<Flight> reversedFlights = new ArrayList<>(flights.findAllSortedByIdentity(SortOrder.ASC));
        Collections.reverse(reversedFlights);
        assertEquals(List.of(delayed, early, late, outside),
                flights.findAllSortedByIdentity(SortOrder.ASC));
        assertEquals(reversedFlights, flights.findAllSortedByIdentity(SortOrder.DESC));

        List<Passenger> reversedPassengers = new ArrayList<>(
                passengers.findAllSortedByIdentity(SortOrder.ASC));
        Collections.reverse(reversedPassengers);
        assertEquals(reversedPassengers, passengers.findAllSortedByIdentity(SortOrder.DESC));
    }

    @Test
    void identitySortIsLexicographicalRatherThanNumerical() {
        GenericRepository<String> repository = new GenericRepository<>("strings", String::toString);
        repository.addAll(List.of("2", "10", "1"));
        assertEquals(List.of("1", "10", "2"), repository.findAllSortedByIdentity(SortOrder.ASC));
    }

    @Test
    void customComparatorDoesNotChangeRepositoryInsertionOrder() {
        List<Flight> original = flights.findAll();
        assertEquals(List.of(delayed, early, late, outside),
                flights.findAllSorted(Comparator.naturalOrder()));
        assertEquals(original, flights.findAll());
    }

    @Test
    void fractionalTicketPricesSortWithoutTruncation() {
        FlightTicket cheaper = new FlightTicket(early, anna, SeatClass.ECONOMY, 100.20);
        FlightTicket dearer = new FlightTicket(early, bohdan, SeatClass.ECONOMY, 100.80);
        GenericRepository<FlightTicket> tickets = new GenericRepository<>(
                "tickets", Lab5Test::ticketIdentity);
        tickets.addAll(List.of(dearer, cheaper));
        assertEquals(List.of(cheaper, dearer),
                tickets.findAllSorted(Comparator.comparingDouble(FlightTicket::price)));
    }

    @Test
    void treeSetTreatsComparatorEqualityAsADuplicate() {
        TreeSet<Passenger> set = new TreeSet<>(Comparator.comparing(Passenger::fullName));
        assertNotEquals(anna, olderAnna);
        assertTrue(set.add(anna));
        assertFalse(set.add(olderAnna));
        assertEquals(1, set.size());
        assertSame(anna, set.first());
    }

    @Test
    void flightTextSearchIgnoresCaseAndReturnsOnlyMatches() {
        assertEquals(List.of(early, late, outside), flights.findByNumberContaining(" ps- "));
        assertTrue(flights.findByNumberContaining("missing").isEmpty());
    }

    @Test
    void flightRangeIncludesBothEndpointsAndExcludesOutsideValues() {
        Flight before = flight("BEFORE", START.minusMinutes(1), FlightStatus.SCHEDULED);
        flights.add(before);
        assertEquals(List.of(early, delayed, late),
                flights.findByDepartureBetween(START, START.plusHours(3)));
        assertEquals(List.of(early), flights.findByDepartureBetween(START, START));
    }

    @Test
    void flightCombinedSearchRequiresBothStatusAndDepartureRange() {
        assertEquals(List.of(early, late), flights.findByStatusAndDepartureBetween(
                FlightStatus.SCHEDULED, START, START.plusHours(3)));
    }

    @Test
    void firstFlightByStatusIsTheEarliestRegardlessOfInsertionOrder() {
        assertSame(early, flights.findFirstByStatus(FlightStatus.SCHEDULED).orElseThrow());
    }

    @Test
    void missingSingleFlightReturnsEmptyOptional() {
        assertTrue(flights.findFirstByStatus(FlightStatus.CANCELLED).isEmpty());
    }

    @Test
    void passengerTextSearchIgnoresCaseAndExcludesOtherNames() {
        assertEquals(List.of(olderAnna, anna), passengers.findByNameContaining(" ANNA "));
        assertTrue(passengers.findByNameContaining("missing").isEmpty());
    }

    @Test
    void passengerRangeIncludesBothEndpointsAndExcludesOutsideValues() {
        assertEquals(List.of(anna, bohdan), passengers.findByBirthDateBetween(
                anna.birthDate(), bohdan.birthDate()));
        assertEquals(List.of(anna), passengers.findByBirthDateBetween(
                anna.birthDate(), anna.birthDate()));
    }

    @Test
    void passengerCombinedSearchRequiresBothNameAndBirthDateRange() {
        assertEquals(List.of(anna), passengers.findByNameAndBirthDateBetween(
                "anna", BIRTH_FROM, BIRTH_TO));
    }

    @Test
    void singlePassengerSearchUsesAnExactNameAndNaturalOrderForTies() {
        assertSame(olderAnna, passengers.findFirstByName(" ANNA KOVALENKO ").orElseThrow());
        assertTrue(passengers.findFirstByName("Anna").isEmpty());
        assertTrue(passengers.findFirstByName("Missing Person").isEmpty());
    }

    @Test
    void distinctNamesAreMappedDeduplicatedSortedAndLimited() {
        assertEquals(List.of("Anna Kovalenko", "Bohdan Melnyk"), passengers.findDistinctNames(10));
        assertEquals(List.of("Anna Kovalenko"), passengers.findDistinctNames(1));
        assertTrue(passengers.findDistinctNames(0).isEmpty());
    }

    @Test
    void groupingProducesExpectedGroupsAndMembers() {
        Map<FlightStatus, List<Flight>> result = flights.groupByStatus();
        assertEquals(2, result.size());
        assertEquals(List.of(outside, late, early), result.get(FlightStatus.SCHEDULED));
        assertEquals(List.of(delayed), result.get(FlightStatus.DELAYED));
    }

    @Test
    void countingCollectorCountsEveryStatus() {
        assertEquals(Map.of(FlightStatus.SCHEDULED, 3L, FlightStatus.DELAYED, 1L), flights.countByStatus());
    }

    @Test
    void totalDurationAddsAllFlightDurations() {
        assertEquals(360, flights.totalDurationMinutes());
    }

    @Test
    void emptyRepositoryHasZeroDurationAndEmptyGroups() {
        FlightRepository empty = new FlightRepository();
        assertEquals(0, empty.totalDurationMinutes());
        assertTrue(empty.groupByStatus().isEmpty());
        assertTrue(empty.countByStatus().isEmpty());
    }

    @Test
    void searchesAndSortsWorkOnEmptyRepositories() {
        FlightRepository emptyFlights = new FlightRepository();
        PassengerRepository emptyPassengers = new PassengerRepository();
        assertTrue(emptyFlights.findAllSortedByIdentity(SortOrder.ASC).isEmpty());
        assertTrue(emptyFlights.findAllByDepartureTime().isEmpty());
        assertTrue(emptyFlights.findByDepartureBetween(START, START).isEmpty());
        assertTrue(emptyFlights.findByStatusAndDepartureBetween(
                FlightStatus.SCHEDULED, START, START).isEmpty());
        assertTrue(emptyPassengers.findAllByBirthDate().isEmpty());
        assertTrue(emptyPassengers.findByNameContaining("Anna").isEmpty());
        assertTrue(emptyPassengers.findByBirthDateBetween(BIRTH_FROM, BIRTH_TO).isEmpty());
        assertTrue(emptyPassengers.findByNameAndBirthDateBetween("Anna", BIRTH_FROM, BIRTH_TO).isEmpty());
        assertTrue(emptyPassengers.findDistinctNames(3).isEmpty());
    }

    @Test
    void consumedStreamCannotBeUsedAgain() {
        try (Stream<Flight> once = flights.stream()) {
            assertEquals(4, once.count());
            assertThrows(IllegalStateException.class, once::count);
        }
        assertEquals(4, flights.stream().count());
    }

    @Test
    void streamIsLazyWhileFindAllIsASnapshot() {
        List<Flight> snapshot = flights.findAll();
        try (Stream<Flight> lazy = flights.stream()) {
            flights.add(flight("NEW", START.plusDays(2), FlightStatus.BOARDING));
            assertEquals(4, snapshot.size());
            assertEquals(5, lazy.count());
        }
    }

    @Test
    void invalidRangesAndNegativeLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> flights.findByDepartureBetween(START.plusDays(1), START));
        assertThrows(IllegalArgumentException.class,
                () -> flights.findByStatusAndDepartureBetween(
                        FlightStatus.SCHEDULED, START.plusDays(1), START));
        assertThrows(IllegalArgumentException.class,
                () -> passengers.findByBirthDateBetween(BIRTH_TO, BIRTH_FROM));
        assertThrows(IllegalArgumentException.class,
                () -> passengers.findByNameAndBirthDateBetween("Anna", BIRTH_TO, BIRTH_FROM));
        assertThrows(IllegalArgumentException.class, () -> passengers.findDistinctNames(-1));
    }

    @Test
    void nullAndBlankSearchArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> flights.findByNumberContaining(" "));
        assertThrows(IllegalArgumentException.class, () -> flights.findFirstByStatus(null));
        assertThrows(IllegalArgumentException.class, () -> flights.findByDepartureBetween(null, START));
        assertThrows(IllegalArgumentException.class, () -> passengers.findByNameContaining(null));
        assertThrows(IllegalArgumentException.class, () -> passengers.findFirstByName(" "));
        assertThrows(IllegalArgumentException.class,
                () -> passengers.findByBirthDateBetween(BIRTH_FROM, null));
        assertThrows(NullPointerException.class, () -> flights.findAllSorted(null));
        assertThrows(NullPointerException.class, () -> flights.findAllSortedByIdentity(null));
    }

    private static Flight flight(String number, LocalDateTime departure, FlightStatus status) {
        return new Flight(number, Airport.of("KBP", "Kyiv"), Airport.of("WAW", "Warsaw"),
                departure, departure.plusMinutes(90), status);
    }

    private static String ticketIdentity(FlightTicket ticket) {
        return ticket.flight().getFlightNumber() + ":" + ticket.passenger().passportSeriesNum();
    }
}
