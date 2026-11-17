package ua.airline.storage;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.Passenger;
import ua.airline.repository.PassengerRepository;
import ua.airline.storage.LoadResult.FieldError;
import ua.airline.storage.LoadResult.Rejection;
import ua.airline.validation.ValidFlightTimes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DataLoaderTest {
    private static ValidatorFactory factory;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void startValidation() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void stopValidation() {
        factory.close();
    }

    @Test
    void reportsStandardAnnotationsWithUkrainianMessages() {
        Passenger invalid = new Passenger("", "А".repeat(101), LocalDate.now().plusDays(1));

        var violations = factory.getValidator().validate(invalid);
        var errors = violations.stream()
                .map(violation -> new FieldError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                new FieldError("passportSeriesNum", "Вкажіть серію та номер паспорта"),
                new FieldError("fullName", "Ім’я пасажира має містити не більше 100 символів"),
                new FieldError("birthDate", "Дата народження не може бути в майбутньому")
        ), errors);

        var flightViolations = factory.getValidator().validate(
                flight("PS/101", LocalDateTime.of(2026, 11, 2, 10, 0),
                        LocalDateTime.of(2026, 11, 2, 11, 0), null));
        assertEquals(Set.of("flightNumber", "status"), flightViolations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet()));
    }

    @Test
    void checksArrivalIsStrictlyAfterDepartureUsingCustomAnnotation() {
        LocalDateTime departure = LocalDateTime.of(2026, 11, 2, 23, 30);
        var validator = factory.getValidator();

        for (LocalDateTime arrival : List.of(departure.minusMinutes(1), departure)) {
            var violations = validator.validate(flight("PS-101", departure, arrival, FlightStatus.SCHEDULED));

            assertEquals(1, violations.size());
            var violation = violations.iterator().next();
            assertEquals(ValidFlightTimes.class, violation.getConstraintDescriptor()
                    .getAnnotation().annotationType());
            assertEquals("arrivalTime", violation.getPropertyPath().toString());
            assertEquals("Час прибуття має бути пізнішим за час вильоту", violation.getMessage());
        }
        assertTrue(validator.validate(flight(
                "PS-101", departure, departure.plusHours(1), FlightStatus.SCHEDULED)).isEmpty());
    }

    @Test
    void loadsValidRowsAfterInvalidRowsAndRejectsDuplicateIdentity() throws IOException {
        Path file = Files.writeString(tempDir.resolve("passengers.json"), """
                [
                  {"passportSeriesNum":"PP1","fullName":"Іван","birthDate":"2000-05-12"},
                  {"passportSeriesNum":"PP2","fullName":""},
                  {"passportSeriesNum":"PP3","fullName":"Марія","birthDate":"bad-date"},
                  {"passportSeriesNum":"PP1","fullName":"Петро","birthDate":"1999-05-12"},
                  {"passportSeriesNum":"PP4","fullName":"Олена","birthDate":"2001-05-12"}
                ]
                """);
        PassengerRepository repository = new PassengerRepository();

        LoadResult result = load(file, repository);

        assertEquals(5, result.readCount());
        assertEquals(2, result.addedCount());
        assertEquals(List.of(2, 3, 4),
                result.rejected().stream().map(Rejection::recordNumber).toList());
        assertEquals(List.of(
                new FieldError("birthDate", "Вкажіть дату народження"),
                new FieldError("fullName", "Вкажіть ім’я пасажира")
        ), result.rejected().getFirst().errors());
        assertEquals("birthDate", result.rejected().get(1).errors().getFirst().field());
        FieldError duplicate = result.rejected().get(2).errors().getFirst();
        assertEquals("identity", duplicate.field());
        assertTrue(duplicate.message().contains("PP1"));
        assertEquals(List.of("Іван", "Олена"),
                repository.findAll().stream().map(Passenger::fullName).toList());
    }

    @Test
    void rejectsIdentityAlreadyInRepositoryWithoutReplacingIt() throws IOException {
        Path file = Files.writeString(tempDir.resolve("passengers.json"), """
                [{"passportSeriesNum":"PP1","fullName":"Петро","birthDate":"1999-05-12"}]
                """);
        Passenger original = new Passenger("PP1", "Іван", LocalDate.of(2000, 5, 12));
        PassengerRepository repository = new PassengerRepository();
        repository.add(original);

        LoadResult result = load(file, repository);

        assertEquals(1, result.readCount());
        assertEquals(0, result.addedCount());
        assertEquals(1, result.rejected().size());
        Rejection rejection = result.rejected().getFirst();
        assertEquals(1, rejection.recordNumber());
        assertEquals("identity", rejection.errors().getFirst().field());
        assertEquals(List.of(original), repository.findAll());
    }

    @Test
    void reportsBrokenJsonAsFileErrorWithoutAddingRecords() throws IOException {
        Path file = Files.writeString(tempDir.resolve("broken.json"), """
                [{"passportSeriesNum":"PP1","fullName":"Іван","birthDate":"2000-05-12"},
                """);
        PassengerRepository repository = new PassengerRepository();

        assertFileError(load(file, repository));
        assertEquals(0, repository.size());
    }

    @Test
    void reportsMissingFileAsFileError() {
        assertFileError(load(tempDir.resolve("missing.yaml"), new PassengerRepository()));
    }

    private LoadResult load(Path file, PassengerRepository repository) {
        return new DataLoader(factory.getValidator())
                .load(file, new FileStorage<>(Passenger.class), repository);
    }

    private static void assertFileError(LoadResult result) {
        assertEquals(0, result.readCount());
        assertEquals(0, result.addedCount());
        assertEquals(1, result.rejected().size());
        Rejection rejection = result.rejected().getFirst();
        assertNull(rejection.recordNumber());
        assertEquals("file", rejection.errors().getFirst().field());
        assertTrue(rejection.errors().getFirst().message().contains("Не вдалося прочитати файл"));
    }

    private static Flight flight(
            String number, LocalDateTime departure, LocalDateTime arrival, FlightStatus status) {
        return new Flight(number, new Airport("KBP", "Київ"), new Airport("WAW", "Варшава"),
                departure, arrival, status);
    }
}
