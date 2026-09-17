package ua.airline.model;

import ua.airline.util.AirlineUtils;
import ua.common.BaseEntity;

import java.util.Objects;

public class FlightTicket extends BaseEntity {

    private final Flight flight;
    private final Passenger passenger;
    private final String seatClass;
    private final double price;

    public FlightTicket(
            Flight flight,
            Passenger passenger,
            String seatClass,
            double price
    ) {
        super();

        this.flight = AirlineUtils.requireNotNull(
                flight,
                "Flight"
        );

        this.passenger = AirlineUtils.requireNotNull(
                passenger,
                "Passenger"
        );

        this.seatClass = AirlineUtils.validateSeatClass(
                seatClass
        );

        this.price = AirlineUtils.requirePositive(
                price,
                "Price"
        );
    }

    public Flight getFlight() {
        return flight;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public String getSeatClass() {
        return seatClass;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlightTicket that = (FlightTicket) o;

        return Objects.equals(flight, that.flight)
                && Objects.equals(passenger, that.passenger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flight, passenger);
    }

    @Override
    public String toString() {
        return "FlightTicket{" +
                "flight=" + flight +
                ", passenger=" + passenger +
                ", seatClass='" + seatClass + '\'' +
                ", price=" + price +
                ", createdAt=" + createdAt +
                '}';
    }
}