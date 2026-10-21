package ua.airline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.Passenger;
import ua.airline.util.AirlineUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        CsvLoader<Passenger> passengerLoader = new CsvLoader<>(
                AirlineCsvParsers.PASSENGER_HEADER,
                AirlineCsvParsers::parsePassenger
        );

        CsvLoader<Airport> airportLoader = new CsvLoader<>(
                AirlineCsvParsers.AIRPORT_HEADER,
                AirlineCsvParsers::parseAirport
        );

        System.out.println("Читання пасажирів із коректними та некоректними даними:");
        List<Passenger> passengers = passengerLoader.load(
                Path.of("data", "passengers.csv")
        );
        System.out.println("Завантажено пасажирів: " + passengers.size());
        for (Passenger passenger : passengers) {
            System.out.println("Пасажир: " + passenger.fullName());
        }

        System.out.println();
        System.out.println("Читання аеропортів із коректними та некоректними даними:");
        List<Airport> airports = airportLoader.load(Path.of("data", "airports.csv"));
        System.out.println("Завантажено аеропортів: " + airports.size());
        for (Airport airport : airports) {
            System.out.println("Аеропорт: " + airport.iataCode() + ", " + airport.cityName());
        }

        System.out.println();
        System.out.println("Спроба прочитати відсутній файл:");
        passengerLoader.load(Path.of("data", "missing-passengers.csv"));
        System.out.println("Після помилки читання програма продовжує роботу.");

        Flight flight = new Flight(
                "PS-101",
                Airport.of("KBP", "Kyiv"),
                Airport.of("WAW", "Warsaw"),
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 1, 11, 30),
                FlightStatus.SCHEDULED
        );

        System.out.println();
        System.out.println("Спроба встановити відсутній статус рейсу:");
        try {
            flight.setStatus(null);
        } catch (IllegalArgumentException e) {
            log.warn("Rejected null flight status", e);
            System.out.println("Статус не змінено: він не може бути відсутнім.");
        }

        System.out.println(
                "Статус після помилки: " + AirlineUtils.statusDescription(flight.getStatus())
        );
        System.out.println("Маршрут: " + AirlineUtils.route(flight));
        System.out.println(
                "Тривалість: " + AirlineUtils.flightDurationMinutes(flight) + " хвилин"
        );

        log.info("Application finished");
        System.out.println("Роботу програми завершено.");
    }
}
