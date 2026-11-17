package ua.airline;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.airline.model.Flight;
import ua.airline.model.Passenger;
import ua.airline.repository.FlightRepository;
import ua.airline.repository.PassengerRepository;
import ua.airline.storage.AppProperties;
import ua.airline.storage.DataLoader;
import ua.airline.storage.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        AppProperties properties = readConfiguration();
        Path passengersFile = properties.requiredPath("data.passengers");
        Path flightsFile = properties.requiredPath("data.flights");
        Path brokenFile = properties.requiredPath("data.broken");
        Path savedFile = properties.requiredPath("data.passengers.saved");

        FileStorage<Passenger> passengerStorage = new FileStorage<>(Passenger.class);
        FileStorage<Flight> flightStorage = new FileStorage<>(Flight.class);
        PassengerRepository passengers = new PassengerRepository();
        FlightRepository flights = new FlightRepository();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            DataLoader loader = new DataLoader(factory.getValidator());
            loader.load(passengersFile, passengerStorage, passengers);
            loader.load(flightsFile, flightStorage, flights);

            log.info("Спроба завантажити пошкоджений файл {}", brokenFile);
            loader.load(brokenFile, passengerStorage, passengers);

            passengers.stream().findFirst().ifPresent(passenger -> {
                passengers.removeByIdentity(passenger.passportSeriesNum());
                log.info("Видалено пасажира з номером паспорта {}", passenger.passportSeriesNum());
            });

            try {
                passengerStorage.save(savedFile, passengers.findAll());
            } catch (IOException | IllegalArgumentException e) {
                log.error("Не вдалося зберегти файл {}. {}", savedFile, e.getMessage());
                return;
            }
            log.info("Збережено {} пасажирів у {}", passengers.size(), savedFile);

            PassengerRepository restored = new PassengerRepository();
            loader.load(savedFile, passengerStorage, restored);
            log.info("Вміст відновленого репозиторію збігається зі збереженим — {}",
                    restored.findAll().equals(passengers.findAll()) ? "так" : "ні");
        }
    }

    private static AppProperties readConfiguration() throws IOException {
        try (InputStream input = Main.class.getResourceAsStream("/app.properties")) {
            if (input == null) {
                throw new IOException("app.properties was not found");
            }
            return new AppProperties(input);
        }
    }
}
