package ua.airline.model;

import ua.airline.util.AirlineUtils;

public record FlightTicket(
        Flight flight,
        Passenger passenger,
        SeatClass seatClass,
        double price
) {
    public FlightTicket {
        flight = AirlineUtils.requireNotNull(
                flight,
                "Flight"
        );

        passenger = AirlineUtils.requireNotNull(
                passenger,
                "Passenger"
        );

        seatClass = AirlineUtils.requireNotNull(
                seatClass,
                "Seat class"
        );

        price = AirlineUtils.requirePositive(
                price,
                "Price"
        );
    }
}