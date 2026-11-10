package ua.airline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.Passenger;
import ua.airline.repository.DuplicateIdentityException;
import ua.airline.repository.GenericRepository;
import ua.airline.util.AirlineUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        GenericRepository<Airport> airportRepository = new GenericRepository<>(
                "airports",
                airport -> airport.iataCode()
        );
        GenericRepository<Passenger> passengerRepository = new GenericRepository<>(
                "passengers",
                passenger -> passenger.passportSeriesNum()
        );
        GenericRepository<Flight> flightRepository = new GenericRepository<>(
                "flights",
                flight -> flight.getFlightNumber()
        );

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
        passengerRepository.addAll(passengers);
        System.out.println("Завантажено пасажирів: " + passengers.size());
        for (Passenger passenger : passengerRepository.findAll()) {
            System.out.println("Пасажир: " + passenger.fullName());
        }

        System.out.println();
        System.out.println("Читання аеропортів із коректними та некоректними даними:");
        List<Airport> airports = airportLoader.load(Path.of("data", "airports.csv"));
        System.out.println("Завантажено аеропортів: " + airports.size());
        for (Airport airport : airports) {
            try {
                airportRepository.add(airport);
            } catch (DuplicateIdentityException e) {
                System.out.println(
                        "Пропущено дублікат із CSV: " + airport.iataCode()
                );
            }
        }
        for (Airport airport : airportRepository.findAll()) {
            System.out.println(
                    "Аеропорт: " + airport.iataCode() + ", " + airport.cityName()
            );
        }

        Flight flight = new Flight(
                "PS-101",
                Airport.of("KBP", "Kyiv"),
                Airport.of("WAW", "Warsaw"),
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 11, 30),
                FlightStatus.SCHEDULED
        );
        flightRepository.add(flight);

        System.out.println();
        System.out.println("Розміри трьох репозиторіїв:");
        System.out.println("Аеропорти: " + airportRepository.size());
        System.out.println("Пасажири: " + passengerRepository.size());
        System.out.println("Рейси: " + flightRepository.size());

        System.out.println();
        System.out.println("Пошук наявних і відсутніх об'єктів:");
        showSearch(airportRepository, "KBP");
        showSearch(airportRepository, "ZZZ");
        showSearch(passengerRepository, "PP10001");
        showSearch(passengerRepository, "MISSING-PASSPORT");
        showSearch(flightRepository, "PS-101");
        showSearch(flightRepository, "MISSING-FLIGHT");

        System.out.println();
        System.out.println("Порівняння add та addIfAbsent для дубліката:");
        Airport duplicateAirport = Airport.of("KBP", "Another city");
        int sizeBeforeDuplicate = airportRepository.size();
        try {
            airportRepository.add(duplicateAirport);
        } catch (DuplicateIdentityException e) {
            System.out.println("add: дублікат відхилено з винятком.");
        }
        boolean added = airportRepository.addIfAbsent(duplicateAirport);
        System.out.println("addIfAbsent: об'єкт додано — " + added);
        System.out.println(
                "Розмір до спроб: " + sizeBeforeDuplicate
                        + ", після спроб: " + airportRepository.size()
        );
        airportRepository.findByIdentity("KBP").ifPresent(
                airport -> System.out.println(
                        "Збережене місто KBP: " + airport.cityName()
                )
        );

        System.out.println();
        System.out.println("Видалення наявного й відсутнього аеропорту:");
        System.out.println(
                "Видалення BER: " + airportRepository.removeByIdentity("BER")
        );
        System.out.println(
                "Видалення відсутнього ZZZ: "
                        + airportRepository.removeByIdentity("ZZZ")
        );

        System.out.println();
        System.out.println("Спроба додати відсутній об'єкт:");
        try {
            airportRepository.add(null);
        } catch (IllegalArgumentException e) {
            System.out.println("Некоректний об'єкт відхилено.");
        }

        System.out.println();
        System.out.println("Зміна статусу рейсу без зміни його identity:");
        flight.setStatus(FlightStatus.BOARDING);
        showSearch(flightRepository, "PS-101");

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

    private static <T> void showSearch(
            GenericRepository<T> repository,
            String identity
    ) {
        repository.findByIdentity(identity).ifPresentOrElse(
                item -> System.out.println("Знайдено " + identity + ": " + item),
                () -> System.out.println("Не знайдено: " + identity)
        );
    }
}
