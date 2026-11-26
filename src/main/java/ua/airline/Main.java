package ua.airline;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.airline.api.ApiServer;
import ua.airline.model.Flight;
import ua.airline.model.Passenger;
import ua.airline.repository.FlightRepository;
import ua.airline.repository.GenericRepository;
import ua.airline.repository.PassengerRepository;
import ua.airline.storage.AppProperties;
import ua.airline.storage.DataLoader;
import ua.airline.storage.FileStorage;
import ua.airline.storage.LoadResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppProperties properties = readConfiguration();
        int port = properties.requiredPort("server.port");
        Path passengersFile = properties.requiredPath("data.passengers");
        Path flightsFile = properties.requiredPath("data.flights");
        Path savedPassengers = properties.requiredPath("data.passengers.saved");
        Path savedFlights = properties.requiredPath("data.flights.saved");
        checkSavePaths(passengersFile, flightsFile, savedPassengers, savedFlights);

        System.out.println("Конфігурацію прочитано з app.properties:");
        System.out.println("  Порт сервера: " + port);
        System.out.println("  Пасажири: " + passengersFile);
        System.out.println("  Рейси: " + flightsFile);
        System.out.println("  Збереження пасажирів: " + savedPassengers);
        System.out.println("  Збереження рейсів: " + savedFlights);

        FileStorage<Passenger> passengerStorage = new FileStorage<>(Passenger.class);
        FileStorage<Flight> flightStorage = new FileStorage<>(Flight.class);
        PassengerRepository passengers = new PassengerRepository();
        FlightRepository flights = new FlightRepository();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            DataLoader loader = new DataLoader(factory.getValidator());
            printLoadResult(passengersFile, loader.load(passengersFile, passengerStorage, passengers));
            printLoadResult(flightsFile, loader.load(flightsFile, flightStorage, flights));
        }

        ApiServer server = new ApiServer(port, flights, passengers);
        try {
            server.start();
        } catch (Exception e) {
            server.stop();
            throw e;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
                System.out.println("Сервер зупинено.");
            } catch (Exception e) {
                log.error("Failed to stop API server", e);
            } finally {
                saveSnapshot(passengerStorage, passengers, savedPassengers);
                saveSnapshot(flightStorage, flights, savedFlights);
            }
        }, "api-shutdown"));
        System.out.printf("Сервер запущено: http://localhost:%d%n", server.port());
        System.out.println("Для зупинки та збереження даних натисніть Ctrl+C.");
    }

    private static void checkSavePaths(Path passengers, Path flights, Path savedPassengers, Path savedFlights) {
        var inputs = java.util.Set.of(passengers.toAbsolutePath().normalize(), flights.toAbsolutePath().normalize());
        Path first = savedPassengers.toAbsolutePath().normalize();
        Path second = savedFlights.toAbsolutePath().normalize();
        if (inputs.contains(first) || inputs.contains(second) || first.equals(second)) {
            throw new IllegalArgumentException("Save paths must be distinct and must not overwrite input files");
        }
    }

    private static <T> void saveSnapshot(FileStorage<T> storage, GenericRepository<T> repository, Path path) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            var items = repository.findAll();
            storage.save(path, items);
            System.out.printf("Збережено %d записів у %s%n", items.size(), path);
            log.info("Saved {} records to {}", items.size(), path);
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Не вдалося зберегти дані у " + path);
            log.error("Failed to save repository to {}", path, e);
        }
    }

    private static void printLoadResult(Path file, LoadResult result) {
        System.out.printf("%s: прочитано %d, додано %d, відкинуто %d%n",
                file, result.readCount(), result.addedCount(), result.rejected().size());
        for (LoadResult.Rejection rejection : result.rejected()) {
            String location = rejection.recordNumber() == null
                    ? "Помилка файлу" : "Запис №" + rejection.recordNumber();
            for (LoadResult.FieldError error : rejection.errors()) {
                String message = switch (error.field()) {
                    case "file" -> "Не вдалося прочитати файл";
                    case "identity" -> "Дублікат ідентичності";
                    case "record" -> "Не вдалося прочитати запис";
                    default -> error.message().startsWith("Failed to parse record.")
                            ? "Не вдалося розібрати значення поля" : error.message();
                };
                System.out.printf("  %s, поле %s: %s%n", location, error.field(), message.replaceAll("\\R", " "));
            }
        }
    }

    private static AppProperties readConfiguration() throws IOException {
        try (InputStream input = Main.class.getResourceAsStream("/app.properties")) {
            if (input == null) {
                throw new IOException("app.properties was not found");
            }
            AppProperties properties = new AppProperties(input);
            log.info("Loaded configuration from app.properties");
            return properties;
        }
    }
}
