package ua.airline;

import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightTicket;
import ua.airline.model.Passenger;
import ua.airline.util.AirlineUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("Створення коректних об'єктів");

        Airport departureAirport = Airport.of(
                "  kbp ",
                "Kyiv"
        );

        Airport arrivalAirport = Airport.of(
                "waw",
                "Warsaw"
        );

        Passenger passenger = Passenger.of(
                "AA123456",
                "Ivan Petrenko",
                LocalDate.of(2000, 5, 15)
        );

        Flight flight = new Flight(
                "PS-101",
                departureAirport,
                arrivalAirport,
                LocalDateTime.of(2026, 10, 10, 10, 0),
                LocalDateTime.of(2026, 10, 10, 12, 0),
                "scheduled"
        );

        FlightTicket ticket = new FlightTicket(
                flight,
                passenger,
                "economy",
                125.50
        );

        System.out.println();
        System.out.println("Нормалізація");
        System.out.println(
                "\"  kbp \" -> \""
                        + departureAirport.getIataCode()
                        + "\""
        );
        System.out.println(
                "\"Ivan@MAIL.com\" -> \""
                        + AirlineUtils.normalizeLowercase(
                        "Ivan@MAIL.com"
                )
                        + "\""
        );

        System.out.println();
        System.out.println("Три порушення правил");

        try {
            Airport.of("KB", "Kyiv");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        try {
            Passenger.of(
                    "BB654321",
                    "Future Passenger",
                    LocalDate.now().plusDays(1)
            );
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        try {
            new FlightTicket(
                    flight,
                    passenger,
                    "business",
                    -50.0
            );
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        System.out.println();
        System.out.println("Перевірка сеттера");
        System.out.println(
                "Статус до помилки: " + flight.getStatus()
        );

        try {
            flight.setStatus("UNKNOWN");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        System.out.println(
                "Статус після помилки: " + flight.getStatus()
        );

        System.out.println();
        System.out.println("equals і hashCode");

        Passenger samePassenger = Passenger.of(
                "AA123456",
                "Another Name",
                LocalDate.of(1999, 1, 1)
        );

        Passenger differentPassenger = Passenger.of(
                "CC999999",
                "Different Passenger",
                LocalDate.of(2001, 2, 20)
        );

        System.out.println(
                "passenger == samePassenger: "
                        + (passenger == samePassenger)
        );
        System.out.println(
                "passenger.equals(samePassenger): "
                        + passenger.equals(samePassenger)
        );
        System.out.println(
                "passenger hashCode: "
                        + passenger.hashCode()
        );
        System.out.println(
                "samePassenger hashCode: "
                        + samePassenger.hashCode()
        );
        System.out.println(
                "passenger.equals(differentPassenger): "
                        + passenger.equals(differentPassenger)
        );

        System.out.println();
        System.out.println("Обчислювані методи");
        System.out.println(
                "Маршрут: " + AirlineUtils.route(flight)
        );
        System.out.println(
                "Тривалість: "
                        + AirlineUtils.flightDurationMinutes(flight)
                        + " хвилин"
        );

        System.out.println();
        System.out.println("Форматування ціни");
        System.out.println(
                AirlineUtils.formatMoney(ticket.getPrice())
        );

        System.out.println();
        System.out.println("Усі сутності");
        System.out.println(passenger);
        System.out.println(departureAirport);
        System.out.println(arrivalAirport);
        System.out.println(flight);
        System.out.println(ticket);

        // ValidationHelper.requireNotBlank("", "Value");
        // Does not compile because ValidationHelper is package-private.
    }
}
