package ua.airline.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Collections Sort Uses Flight Numbers")
    void collectionsSortUsesFlightNumbers() {
        List<Flight> result = new ArrayList<>(flights.findAll());
        Collections.sort(result);
        assertEquals(List.of(delayed, early, late, outside), result);
    }

    @Test
    @DisplayName("Collections Sort Uses Passenger Natural Order")
    void collectionsSortUsesPassengerNaturalOrder() {
        List<Passenger> result = new ArrayList<>(passengers.findAll());
        Collections.sort(result);
        assertEquals(List.of(olderAnna, anna, bohdan), result);
    }

    @Test
    @DisplayName("Passenger Natural Order Breaks Ties Using All Record Fields")
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
    @DisplayName("Flight Natural Order Matches Identity Based Equality")
    void flightNaturalOrderMatchesIdentityBasedEquality() {
        Flight sameNumber = flight(early.getFlightNumber(), START.plusDays(5), FlightStatus.CANCELLED);
        assertEquals(early, sameNumber);
        assertEquals(0, early.compareTo(sameNumber));
    }

    @Test
    @DisplayName("Multi Level Comparator Sorts Statuses And Reverses Only Departure Time")
    void multiLevelComparatorSortsStatusesAndReversesOnlyDepartureTime() {
        assertEquals(List.of(outside, late, early, delayed),
                flights.findAllByStatusAndLatestDeparture());
    }

    @Test
    @DisplayName("Departure Time Sort Uses Flight Number For Ties")
    void departureTimeSortUsesFlightNumberForTies() {
        Flight tied = flight("AA-001", START, FlightStatus.CANCELLED);
        flights.add(tied);
        assertEquals(List.of(tied, early, delayed, late, outside), flights.findAllByDepartureTime());
    }

    @Test
    @DisplayName("Passenger Name Sort Breaks Ties By Natural Order")
    void passengerNameSortBreaksTiesByNaturalOrder() {
        assertEquals(List.of(olderAnna, anna, bohdan), passengers.findAllByName());
    }

    @Test
    @DisplayName("Passenger Birth Date Sort Orders Oldest First")
    void passengerBirthDateSortOrdersOldestFirst() {
        assertEquals(List.of(olderAnna, anna, bohdan), passengers.findAllByBirthDate());
    }

    @Test
    @DisplayName("Flight Identity Descending Reverses Ascending")
    void flightIdentityDescendingReversesAscending() {
        List<Flight> ascending = flights.findAllSortedByIdentity(SortOrder.ASC);
        assertEquals(List.of(delayed, early, late, outside), ascending);
        assertEquals(ascending.reversed(), flights.findAllSortedByIdentity(SortOrder.DESC));
    }

    @Test
    @DisplayName("Passenger Identity Descending Reverses Ascending")
    void passengerIdentityDescendingReversesAscending() {
        List<Passenger> ascending = passengers.findAllSortedByIdentity(SortOrder.ASC);
        assertEquals(List.of(olderAnna, anna, bohdan), ascending);
        assertEquals(ascending.reversed(), passengers.findAllSortedByIdentity(SortOrder.DESC));
    }


    @Test
    @DisplayName("Identity Sort Is Lexicographical Rather Than Numerical")
    void identitySortIsLexicographicalRatherThanNumerical() {
        GenericRepository<String> repository = new GenericRepository<>("strings", String::toString);
        repository.addAll(List.of("2", "10", "1"));
        assertEquals(List.of("1", "10", "2"), repository.findAllSortedByIdentity(SortOrder.ASC));
    }

    @Test
    @DisplayName("Custom Comparator Does Not Change Repository Insertion Order")
    void customComparatorDoesNotChangeRepositoryInsertionOrder() {
        List<Flight> original = flights.findAll();
        assertEquals(List.of(delayed, early, late, outside),
                flights.findAllSorted(Comparator.naturalOrder()));
        assertEquals(original, flights.findAll());
    }

    @Test
    @DisplayName("Fractional Ticket Prices Sort Without Truncation")
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
    @DisplayName("Tree Set Treats Comparator Equality As ADuplicate")
    void treeSetTreatsComparatorEqualityAsADuplicate() {
        TreeSet<Passenger> set = new TreeSet<>(Comparator.comparing(Passenger::fullName));
        assertNotEquals(anna, olderAnna);
        assertTrue(set.add(anna));
        assertFalse(set.add(olderAnna));
        assertEquals(1, set.size());
        assertSame(anna, set.first());
    }

    @Test
    @DisplayName("Flight Text Search Ignores Case And Returns Only Matches")
    void flightTextSearchIgnoresCaseAndReturnsOnlyMatches() {
        assertEquals(List.of(early, late, outside), flights.findByNumberContaining(" ps- "));
        assertTrue(flights.findByNumberContaining("missing").isEmpty());
    }

    @Test
    @DisplayName("Flight Range Includes Both Endpoints And Excludes Outside Values")
    void flightRangeIncludesBothEndpointsAndExcludesOutsideValues() {
        Flight before = flight("BEFORE", START.minusMinutes(1), FlightStatus.SCHEDULED);
        flights.add(before);
        assertEquals(List.of(early, delayed, late),
                flights.findByDepartureBetween(START, START.plusHours(3)));
        assertEquals(List.of(early), flights.findByDepartureBetween(START, START));
    }

    @Test
    @DisplayName("Flight Combined Search Requires Both Status And Departure Range")
    void flightCombinedSearchRequiresBothStatusAndDepartureRange() {
        assertEquals(List.of(early, late), flights.findByStatusAndDepartureBetween(
                FlightStatus.SCHEDULED, START, START.plusHours(3)));
    }

    @Test
    @DisplayName("First Flight By Status Is The Earliest Regardless Of Insertion Order")
    void firstFlightByStatusIsTheEarliestRegardlessOfInsertionOrder() {
        assertSame(early, flights.findFirstByStatus(FlightStatus.SCHEDULED).orElseThrow());
    }

    @Test
    @DisplayName("Missing Single Flight Returns Empty Optional")
    void missingSingleFlightReturnsEmptyOptional() {
        assertTrue(flights.findFirstByStatus(FlightStatus.CANCELLED).isEmpty());
    }

    @Test
    @DisplayName("Passenger Text Search Ignores Case And Excludes Other Names")
    void passengerTextSearchIgnoresCaseAndExcludesOtherNames() {
        assertEquals(List.of(olderAnna, anna), passengers.findByNameContaining(" ANNA "));
        assertTrue(passengers.findByNameContaining("missing").isEmpty());
    }

    @Test
    @DisplayName("Passenger Range Includes Both Endpoints And Excludes Outside Values")
    void passengerRangeIncludesBothEndpointsAndExcludesOutsideValues() {
        assertEquals(List.of(anna, bohdan), passengers.findByBirthDateBetween(
                anna.birthDate(), bohdan.birthDate()));
        assertEquals(List.of(anna), passengers.findByBirthDateBetween(
                anna.birthDate(), anna.birthDate()));
    }

    @Test
    @DisplayName("Passenger Combined Search Requires Both Name And Birth Date Range")
    void passengerCombinedSearchRequiresBothNameAndBirthDateRange() {
        assertEquals(List.of(anna), passengers.findByNameAndBirthDateBetween(
                "anna", BIRTH_FROM, BIRTH_TO));
    }

    @Test
    @DisplayName("Single Passenger Search Uses An Exact Name And Natural Order For Ties")
    void singlePassengerSearchUsesAnExactNameAndNaturalOrderForTies() {
        assertSame(olderAnna, passengers.findFirstByName(" ANNA KOVALENKO ").orElseThrow());
        assertTrue(passengers.findFirstByName("Anna").isEmpty());
        assertTrue(passengers.findFirstByName("Missing Person").isEmpty());
    }

    @Test
    @DisplayName("Distinct Names Are Mapped Deduplicated Sorted And Limited")
    void distinctNamesAreMappedDeduplicatedSortedAndLimited() {
        assertEquals(List.of("Anna Kovalenko", "Bohdan Melnyk"), passengers.findDistinctNames(10));
        assertEquals(List.of("Anna Kovalenko"), passengers.findDistinctNames(1));
        assertTrue(passengers.findDistinctNames(0).isEmpty());
    }

    @Test
    @DisplayName("Grouping Produces Expected Groups And Members")
    void groupingProducesExpectedGroupsAndMembers() {
        Map<FlightStatus, List<Flight>> result = flights.groupByStatus();
        assertEquals(2, result.size());
        assertEquals(List.of(outside, late, early), result.get(FlightStatus.SCHEDULED));
        assertEquals(List.of(delayed), result.get(FlightStatus.DELAYED));
    }

    @Test
    @DisplayName("Counting Collector Counts Every Status")
    void countingCollectorCountsEveryStatus() {
        assertEquals(Map.of(FlightStatus.SCHEDULED, 3L, FlightStatus.DELAYED, 1L), flights.countByStatus());
    }

    @Test
    @DisplayName("Total Duration Adds All Flight Durations")
    void totalDurationAddsAllFlightDurations() {
        assertEquals(360, flights.totalDurationMinutes());
    }

    @Test
    @DisplayName("Empty Repository Has Zero Duration")
    void emptyRepositoryHasZeroDuration() {
        assertEquals(0, new FlightRepository().totalDurationMinutes());
    }

    @Test
    @DisplayName("Empty Repository Has No Status Groups")
    void emptyRepositoryHasNoStatusGroups() {
        assertTrue(new FlightRepository().groupByStatus().isEmpty());
    }

    @Test
    @DisplayName("Empty Repository Has No Status Counts")
    void emptyRepositoryHasNoStatusCounts() {
        assertTrue(new FlightRepository().countByStatus().isEmpty());
    }


    @Test
    @DisplayName("Empty Flight Identity Sort Returns Empty List")
    void emptyFlightIdentitySortReturnsEmptyList() {
        assertTrue(new FlightRepository().findAllSortedByIdentity(SortOrder.ASC).isEmpty());
    }

    @Test
    @DisplayName("Empty Flight Departure Sort Returns Empty List")
    void emptyFlightDepartureSortReturnsEmptyList() {
        assertTrue(new FlightRepository().findAllByDepartureTime().isEmpty());
    }

    @Test
    @DisplayName("Empty Flight Range Search Returns Empty List")
    void emptyFlightRangeSearchReturnsEmptyList() {
        assertTrue(new FlightRepository().findByDepartureBetween(START, START).isEmpty());
    }

    @Test
    @DisplayName("Empty Flight Combined Search Returns Empty List")
    void emptyFlightCombinedSearchReturnsEmptyList() {
        assertTrue(new FlightRepository().findByStatusAndDepartureBetween(FlightStatus.SCHEDULED, START, START).isEmpty());
    }

    @Test
    @DisplayName("Empty Passenger Birth Date Sort Returns Empty List")
    void emptyPassengerBirthDateSortReturnsEmptyList() {
        assertTrue(new PassengerRepository().findAllByBirthDate().isEmpty());
    }

    @Test
    @DisplayName("Empty Passenger Name Search Returns Empty List")
    void emptyPassengerNameSearchReturnsEmptyList() {
        assertTrue(new PassengerRepository().findByNameContaining("Anna").isEmpty());
    }

    @Test
    @DisplayName("Empty Passenger Range Search Returns Empty List")
    void emptyPassengerRangeSearchReturnsEmptyList() {
        assertTrue(new PassengerRepository().findByBirthDateBetween(BIRTH_FROM, BIRTH_TO).isEmpty());
    }

    @Test
    @DisplayName("Empty Passenger Combined Search Returns Empty List")
    void emptyPassengerCombinedSearchReturnsEmptyList() {
        assertTrue(new PassengerRepository().findByNameAndBirthDateBetween("Anna", BIRTH_FROM, BIRTH_TO).isEmpty());
    }

    @Test
    @DisplayName("Empty Passenger Distinct Names Returns Empty List")
    void emptyPassengerDistinctNamesReturnsEmptyList() {
        assertTrue(new PassengerRepository().findDistinctNames(3).isEmpty());
    }


    @Test
    @DisplayName("Consumed Stream Cannot Be Used Again")
    void consumedStreamCannotBeUsedAgain() {
        try (Stream<Flight> once = flights.stream()) {
            assertEquals(4, once.count());
            assertThrows(IllegalStateException.class, once::count);
        }
        assertEquals(4, flights.stream().count());
    }

    @Test
    @DisplayName("Stream Is Lazy While Find All Is ASnapshot")
    void streamUsesSnapshotDuringConcurrentUpdates() {
        List<Flight> snapshot = flights.findAll();
        try (Stream<Flight> lazy = flights.stream()) {
            flights.add(flight("NEW", START.plusDays(2), FlightStatus.BOARDING));
            assertEquals(4, snapshot.size());
            assertEquals(4, lazy.count());
        }
    }

    @Test
    @DisplayName("Rejects Reversed Flight Range")
    void rejectsReversedFlightRange() {
        assertThrows(IllegalArgumentException.class, () -> flights.findByDepartureBetween(START.plusDays(1), START));
    }

    @Test
    @DisplayName("Rejects Reversed Combined Flight Range")
    void rejectsReversedCombinedFlightRange() {
        assertThrows(IllegalArgumentException.class, () -> flights.findByStatusAndDepartureBetween(FlightStatus.SCHEDULED, START.plusDays(1), START));
    }

    @Test
    @DisplayName("Rejects Reversed Passenger Range")
    void rejectsReversedPassengerRange() {
        assertThrows(IllegalArgumentException.class, () -> passengers.findByBirthDateBetween(BIRTH_TO, BIRTH_FROM));
    }

    @Test
    @DisplayName("Rejects Reversed Combined Passenger Range")
    void rejectsReversedCombinedPassengerRange() {
        assertThrows(IllegalArgumentException.class, () -> passengers.findByNameAndBirthDateBetween("Anna", BIRTH_TO, BIRTH_FROM));
    }

    @Test
    @DisplayName("Rejects Negative Distinct Name Limit")
    void rejectsNegativeDistinctNameLimit() {
        assertThrows(IllegalArgumentException.class, () -> passengers.findDistinctNames(-1));
    }


    @Test
    @DisplayName("Rejects Blank Flight Number Search")
    void rejectsBlankFlightNumberSearch() {
        assertThrows(IllegalArgumentException.class, () -> flights.findByNumberContaining(" "));
    }

    @Test
    @DisplayName("Rejects Null Flight Status Search")
    void rejectsNullFlightStatusSearch() {
        assertThrows(IllegalArgumentException.class, () -> flights.findFirstByStatus(null));
    }

    @Test
    @DisplayName("Rejects Null Flight Range Start")
    void rejectsNullFlightRangeStart() {
        assertThrows(IllegalArgumentException.class, () -> flights.findByDepartureBetween(null, START));
    }

    @Test
    @DisplayName("Rejects Null Passenger Name Search")
    void rejectsNullPassengerNameSearch() {
        assertThrows(IllegalArgumentException.class, () -> passengers.findByNameContaining(null));
    }

    @Test
    @DisplayName("Rejects Blank Single Passenger Search")
    void rejectsBlankSinglePassengerSearch() {
        assertThrows(IllegalArgumentException.class, () -> passengers.findFirstByName(" "));
    }

    @Test
    @DisplayName("Rejects Null Passenger Range End")
    void rejectsNullPassengerRangeEnd() {
        assertThrows(IllegalArgumentException.class, () -> passengers.findByBirthDateBetween(BIRTH_FROM, null));
    }

    @Test
    @DisplayName("Rejects Null Sorting Comparator")
    void rejectsNullSortingComparator() {
        assertThrows(NullPointerException.class, () -> flights.findAllSorted(null));
    }

    @Test
    @DisplayName("Rejects Null Identity Sort Order")
    void rejectsNullIdentitySortOrder() {
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
