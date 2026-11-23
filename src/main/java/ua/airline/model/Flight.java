package ua.airline.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ua.airline.util.AirlineUtils;
import ua.airline.validation.ValidFlightTimes;
import ua.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.Objects;

@ValidFlightTimes
@JsonIgnoreProperties("createdAt")
public final class Flight extends BaseEntity implements Comparable<Flight> {

    @NotBlank(message = "Вкажіть номер рейсу")
    @Pattern(regexp = "[A-Za-z0-9-]*",
            message = "Номер рейсу може містити лише латинські літери, цифри та дефіс")
    private final String flightNumber;

    @NotNull(message = "Вкажіть аеропорт вильоту")
    private final Airport departureAirport;

    @NotNull(message = "Вкажіть аеропорт прибуття")
    private final Airport arrivalAirport;

    @NotNull(message = "Вкажіть час вильоту")
    private final LocalDateTime departureTime;

    @NotNull(message = "Вкажіть час прибуття")
    private final LocalDateTime arrivalTime;

    @NotNull(message = "Вкажіть статус рейсу")
    private FlightStatus status;

    @JsonCreator
    public Flight(
            @JsonProperty("flightNumber") String flightNumber,
            @JsonProperty("departureAirport") Airport departureAirport,
            @JsonProperty("arrivalAirport") Airport arrivalAirport,
            @JsonProperty("departureTime") LocalDateTime departureTime,
            @JsonProperty("arrivalTime") LocalDateTime arrivalTime,
            @JsonProperty("status") FlightStatus status
    ) {
        super();
        if (departureAirport != null && arrivalAirport != null) {
            AirlineUtils.requireDifferentAirports(departureAirport, arrivalAirport);
        }

        this.flightNumber = flightNumber;
        this.departureAirport = departureAirport;
        this.arrivalAirport = arrivalAirport;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.status = status;
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
    public int compareTo(Flight other) {
        return flightNumber.compareTo(other.flightNumber);
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
