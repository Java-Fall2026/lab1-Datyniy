package ua.airline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.FlightTicket;
import ua.airline.model.Passenger;
import ua.airline.model.SeatClass;
import ua.airline.util.AirlineUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AirlineModelTest {

    @Test
    @DisplayName("Creates Valid Passenger")
    void createsValidPassenger() {
        Passenger passenger = Passenger.of(
                "PP10001", "Ivan Petrenko", LocalDate.of(2000, 5, 12)
        );

        assertEquals("PP10001", passenger.passportSeriesNum());
        assertEquals("Ivan Petrenko", passenger.fullName());
        assertEquals(LocalDate.of(2000, 5, 12), passenger.birthDate());
    }

    @Test
    @DisplayName("Creates Airport With Normalized Code")
    void createsAirportWithNormalizedCode() {
        Airport airport = Airport.of("  kbp  ", "Kyiv");

        assertEquals("KBP", airport.iataCode());
        assertEquals("Kyiv", airport.cityName());
    }

    @Test
    @DisplayName("Creates Valid Ticket")
    void createsValidTicket() {
        Flight flight = sampleFlight();
        Passenger passenger = Passenger.of(
                "PP10001", "Ivan Petrenko", LocalDate.of(2000, 5, 12)
        );

        FlightTicket ticket = new FlightTicket(
                flight, passenger, SeatClass.ECONOMY, 125.50
        );

        assertSame(flight, ticket.flight());
        assertSame(passenger, ticket.passenger());
        assertEquals(SeatClass.ECONOMY, ticket.seatClass());
        assertEquals(125.50, ticket.price(), 0.001);
    }

    @Test
    @DisplayName("Record Constructor Still Throws Unchecked Validation Exception")
    void recordConstructorStillThrowsUncheckedValidationException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Airport.of("12A", "Kyiv")
        );
    }

    @Test
    @DisplayName("Calculates Flight Duration Across Midnight")
    void calculatesFlightDurationAcrossMidnight() {
        assertEquals(90L, AirlineUtils.flightDurationMinutes(sampleFlight()));
    }

    @Test
    @DisplayName("Builds Flight Route")
    void buildsFlightRoute() {
        assertEquals("KBP → WAW", AirlineUtils.route(sampleFlight()));
    }

    @Test
    @DisplayName("Invalid Setter Value Does Not Change Existing Status")
    void invalidSetterValueDoesNotChangeExistingStatus() {
        Flight flight = sampleFlight();

        assertThrows(IllegalArgumentException.class, () -> flight.setStatus(null));

        assertEquals(FlightStatus.SCHEDULED, flight.getStatus());
    }

    private Flight sampleFlight() {
        return new Flight(
                "PS-101",
                Airport.of("KBP", "Kyiv"),
                Airport.of("WAW", "Warsaw"),
                LocalDateTime.of(2026, 11, 1, 23, 30),
                LocalDateTime.of(2026, 11, 2, 1, 0),
                FlightStatus.SCHEDULED
        );
    }
}
