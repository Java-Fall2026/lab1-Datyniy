package ua.airline.model;

import ua.airline.util.AirlineUtils;
import ua.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

public class Flight extends BaseEntity {

    private final String flightNumber;
    private final Airport departureAirport;
    private final Airport arrivalAirport;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private FlightStatus status;

    public Flight(
            String flightNumber,
            Airport departureAirport,
            Airport arrivalAirport,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            FlightStatus status
    ) {
        super();

        this.flightNumber = AirlineUtils.requireNotBlank(
                flightNumber,
                "Flight number"
        );

        this.departureAirport = AirlineUtils.requireNotNull(
                departureAirport,
                "Departure airport"
        );

        this.arrivalAirport = AirlineUtils.requireNotNull(
                arrivalAirport,
                "Arrival airport"
        );

        AirlineUtils.requireDifferentAirports(
                this.departureAirport,
                this.arrivalAirport
        );

        this.departureTime = AirlineUtils.requireNotNull(
                departureTime,
                "Departure time"
        );

        this.arrivalTime = AirlineUtils.requireNotNull(
                arrivalTime,
                "Arrival time"
        );

        AirlineUtils.requireArrivalAfterDeparture(
                this.departureTime,
                this.arrivalTime
        );

        setStatus(status);
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public Airport getDepartureAirport() {
        return departureAirport;
    }

    public Airport getArrivalAirport() {
        return arrivalAirport;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public final void setStatus(FlightStatus status) {
        this.status = AirlineUtils.requireNotNull(
                status,
                "Flight status"
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Flight flight = (Flight) o;
        return Objects.equals(flightNumber, flight.flightNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightNumber);
    }

    @Override
    public String toString() {
        return "Flight{" +
                "flightNumber='" + flightNumber + '\'' +
                ", departureAirport=" + departureAirport +
                ", arrivalAirport=" + arrivalAirport +
                ", departureTime=" + departureTime +
                ", arrivalTime=" + arrivalTime +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}