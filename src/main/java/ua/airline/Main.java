package ua.airline;

import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.FlightTicket;
import ua.airline.model.Passenger;
import ua.airline.model.SeatClass;
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
                FlightStatus.SCHEDULED
        );

        FlightTicket ticket = new FlightTicket(
                flight,
                passenger,
                SeatClass.ECONOMY,
                125.50
        );

        System.out.println();
        System.out.println("Звернення до компонентів record");
        System.out.println("Код аеропорту: " + departureAirport.iataCode());
        System.out.println("Місто: " + departureAirport.cityName());
        System.out.println("Пасажир: " + passenger.fullName());
        System.out.println("Дата народження: " + passenger.birthDate());
        System.out.println(
                "Клас обслуговування: "
                        + ticket.seatClass().getDisplayName()
        );
        System.out.println("Ціна квитка: " + ticket.price());

        System.out.println();
        System.out.println("Нормалізація");
        System.out.println(
                "\"  kbp \" -> \"" + departureAirport.iataCode() + "\""
        );
        System.out.println(
                "\"Ivan@MAIL.com\" -> \""
                        + AirlineUtils.normalizeLowercase("Ivan@MAIL.com")
                        + "\""
        );

        System.out.println();
        System.out.println("Усі статуси та перший switch-вираз");

        for (FlightStatus status : FlightStatus.values()) {
            System.out.println(
                    status + " — " + AirlineUtils.statusDescription(status)
            );
        }

        System.out.println();
        System.out.println("Класи обслуговування та другий switch-вираз");

        for (SeatClass seatClass : SeatClass.values()) {
            System.out.println(
                    seatClass
                            + " — "
                            + seatClass.getDisplayName()
                            + ": "
                            + AirlineUtils.serviceDescription(seatClass)
            );
        }

        System.out.println();
        System.out.println("Зміна статусу рейсу");
        System.out.println(
                "До зміни: "
                        + AirlineUtils.statusDescription(flight.getStatus())
        );

        flight.setStatus(FlightStatus.BOARDING);

        System.out.println(
                "Після зміни: "
                        + AirlineUtils.statusDescription(flight.getStatus())
        );

        System.out.println();
        System.out.println("Неправильне значення для valueOf");

        try {
            FlightStatus.valueOf("UNKNOWN");
        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Перехоплено IllegalArgumentException: "
                            + "статусу UNKNOWN не існує."
            );
        }

        System.out.println();
        System.out.println("Перевірка відсутнього статусу");

        try {
            flight.setStatus(null);
        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Перехоплено IllegalArgumentException: "
                            + "статус не може бути відсутнім."
            );
        }

        System.out.println(
                "Статус після помилки: "
                        + AirlineUtils.statusDescription(flight.getStatus())
        );

        System.out.println();
        System.out.println("Невалідні дані в компактних конструкторах");

        try {
            Airport.of("KB", "Kyiv");
        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Перехоплено IllegalArgumentException: "
                            + "код аеропорту має містити три латинські літери."
            );
        }

        try {
            Passenger.of(
                    "BB654321",
                    "Future Passenger",
                    LocalDate.now().plusDays(1)
            );
        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Перехоплено IllegalArgumentException: "
                            + "дата народження не може бути в майбутньому."
            );
        }

        try {
            new FlightTicket(
                    flight,
                    passenger,
                    SeatClass.BUSINESS,
                    -50.0
            );
        } catch (IllegalArgumentException e) {
            System.out.println(
                    "Перехоплено IllegalArgumentException: "
                            + "ціна квитка має бути більшою за нуль."
            );
        }

        System.out.println();
        System.out.println("Порівняння двох однакових record-об'єктів");

        FlightTicket sameTicket = new FlightTicket(
                flight,
                passenger,
                SeatClass.ECONOMY,
                125.50
        );

        System.out.println(
                "Посилання на той самий об'єкт: " + (ticket == sameTicket)
        );
        System.out.println(
                "Однакові дані: " + ticket.equals(sameTicket)
        );
        System.out.println(
                "Хеш-код першого квитка: " + ticket.hashCode()
        );
        System.out.println(
                "Хеш-код другого квитка: " + sameTicket.hashCode()
        );

        System.out.println();
        System.out.println("Обчислювані методи з першої лабораторної");
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
                AirlineUtils.formatMoney(ticket.price())
        );

        System.out.println();
        System.out.println("Згенерований toString для records");
        System.out.println(departureAirport);
        System.out.println(arrivalAirport);
        System.out.println(passenger);
        System.out.println(ticket);

        System.out.println();
        System.out.println("Рейс залишається звичайним класом");
        System.out.println(flight);
    }
}