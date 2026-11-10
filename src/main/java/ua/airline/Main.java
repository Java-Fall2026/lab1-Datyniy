package ua.airline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.Passenger;
import ua.airline.model.FlightTicket;
import ua.airline.model.SeatClass;
import ua.airline.repository.DuplicateIdentityException;
import ua.airline.repository.GenericRepository;
import ua.airline.repository.FlightRepository;
import ua.airline.repository.PassengerRepository;
import ua.airline.repository.SortOrder;
import ua.airline.util.AirlineUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        GenericRepository<Airport> airportRepository = new GenericRepository<>(
                "airports",
                Airport::iataCode
        );
        PassengerRepository passengerRepository = new PassengerRepository();
        FlightRepository flightRepository = new FlightRepository();

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

        demonstrateLab5(flightRepository, passengerRepository);

        log.info("Application finished");
        System.out.println("Роботу програми завершено.");
    }

    private static void demonstrateLab5(
            FlightRepository flights, PassengerRepository passengers) {
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 9, 0);
        flights.addAll(List.of(
                new Flight("PS-303", Airport.of("KBP", "Kyiv"), Airport.of("BER", "Berlin"),
                        start.plusHours(4), start.plusHours(6), FlightStatus.SCHEDULED),
                new Flight("PS-202", Airport.of("KBP", "Kyiv"), Airport.of("WAW", "Warsaw"),
                        start, start.plusMinutes(90), FlightStatus.SCHEDULED),
                new Flight("LO-404", Airport.of("WAW", "Warsaw"), Airport.of("BER", "Berlin"),
                        start.plusDays(1), start.plusDays(1).plusHours(1), FlightStatus.DELAYED)
        ));
        Passenger first = Passenger.of("DEMO-02", "Олена Коваль", LocalDate.of(2000, 1, 1));
        Passenger second = Passenger.of("DEMO-01", "Олена Коваль", LocalDate.of(1990, 5, 12));
        passengers.addAll(List.of(first, second));

        System.out.println("\nЛР5: природний порядок рейсів і пасажирів:");
        List<Flight> naturalFlights = new ArrayList<>(flights.findAll());
        Collections.sort(naturalFlights);
        log.info("Sorted flights by natural order: {} items", naturalFlights.size());
        System.out.println(naturalFlights);
        List<Passenger> naturalPassengers = new ArrayList<>(passengers.findAll());
        Collections.sort(naturalPassengers);
        log.info("Sorted passengers by natural order: {} items", naturalPassengers.size());
        System.out.println(naturalPassengers);

        System.out.println("\nРейси за часом вильоту:");
        System.out.println(flights.findAllByDepartureTime());
        System.out.println("Рейси за статусом, потім за часом вильоту від пізнього до раннього:");
        System.out.println(flights.findAllByStatusAndLatestDeparture());
        System.out.println("Пасажири за ім'ям:");
        System.out.println(passengers.findAllByName());
        System.out.println("Пасажири за датою народження:");
        System.out.println(passengers.findAllByBirthDate());

        System.out.println("\nРейси за ключем за зростанням:");
        System.out.println(flights.findAllSortedByIdentity(SortOrder.ASC));
        System.out.println("Рейси за ключем за спаданням:");
        System.out.println(flights.findAllSortedByIdentity(SortOrder.DESC));
        System.out.println("Пасажири за ключем за зростанням:");
        System.out.println(passengers.findAllSortedByIdentity(SortOrder.ASC));
        System.out.println("Пасажири за ключем за спаданням:");
        System.out.println(passengers.findAllSortedByIdentity(SortOrder.DESC));

        TreeSet<Passenger> byName = new TreeSet<>(Comparator.comparing(Passenger::fullName));
        byName.add(first);
        byName.add(second);
        System.out.println("\nДва різні пасажири з однаковим ім'ям у TreeSet:");
        System.out.println("Рівність через equals: " + first.equals(second));
        System.out.println("Кількість елементів: " + byName.size());

        System.out.println("\nРейси, номер яких містить PS:");
        System.out.println(flights.findByNumberContaining("ps"));
        System.out.println("Вильоти в межах указаного дня:");
        System.out.println(flights.findByDepartureBetween(start, start.plusHours(14)));
        System.out.println("Заплановані рейси в межах указаного дня:");
        System.out.println(flights.findByStatusAndDepartureBetween(
                FlightStatus.SCHEDULED, start, start.plusHours(14)));
        System.out.println("Перший запланований рейс:");
        flights.findFirstByStatus(FlightStatus.SCHEDULED)
                .ifPresent(f -> System.out.println(f.getFlightNumber()));
        flights.findFirstByStatus(FlightStatus.CANCELLED).ifPresentOrElse(
                f -> System.out.println("Скасований рейс: " + f.getFlightNumber()),
                () -> System.out.println("Скасованих рейсів немає."));

        LocalDate birthFrom = LocalDate.of(1995, 1, 1);
        LocalDate birthTo = LocalDate.of(2005, 12, 31);
        System.out.println("\nПасажири, чиє ім'я містить Олена:");
        System.out.println(passengers.findByNameContaining("олена"));
        System.out.println("Пасажири за діапазоном дат народження:");
        System.out.println(passengers.findByBirthDateBetween(birthFrom, birthTo));
        System.out.println("Пасажири за ім'ям та діапазоном дат народження:");
        System.out.println(passengers.findByNameAndBirthDateBetween("Олена", birthFrom, birthTo));
        passengers.findFirstByName("Олена Коваль")
                .ifPresent(p -> System.out.println("Знайдено: " + p));
        System.out.println(passengers.findFirstByName("Відсутній Пасажир")
                .map(p -> "Знайдено: " + p.fullName())
                .orElse("Пасажира з таким ім'ям немає."));
        System.out.println("Перші три унікальні імена за алфавітом:");
        System.out.println(passengers.findDistinctNames(3));

        System.out.println("\nГрупування рейсів за статусом:");
        System.out.println(flights.groupByStatus());
        System.out.println("Кількість рейсів за статусом:");
        System.out.println(flights.countByStatus());
        System.out.println("Загальна тривалість польотів, хв: " + flights.totalDurationMinutes());
        System.out.println("Тривалість у порожньому репозиторії, хв: "
                + new FlightRepository().totalDurationMinutes());

        Flight ticketFlight = naturalFlights.getFirst();
        List<FlightTicket> tickets = List.of(
                new FlightTicket(ticketFlight, first, SeatClass.ECONOMY, 100.80),
                new FlightTicket(ticketFlight, second, SeatClass.ECONOMY, 100.20));
        List<FlightTicket> byPrice = tickets.stream()
                .sorted(Comparator.comparingDouble(FlightTicket::price))
                .toList();
        log.info("Sorted tickets by price ASC: {} items", byPrice.size());
        System.out.println("Ціни квитків за зростанням: "
                + byPrice.stream().map(FlightTicket::price).toList());

        try (Stream<Flight> once = flights.stream()) {
            System.out.println("\nПерший прохід стріму, кількість рейсів: " + once.count());
            try {
                once.count();
            } catch (IllegalStateException e) {
                log.error("Cannot consume the same stream twice", e);
                System.out.println("Повторне використання стріму спричинило перехоплений виняток.");
            }
        }
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
