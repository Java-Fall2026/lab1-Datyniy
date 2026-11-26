package ua.airline.storage;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.airline.model.Passenger;
import ua.airline.repository.PassengerRepository;
import ua.airline.storage.LoadResult.FieldError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataLoaderTest {
    private static ValidatorFactory factory;
    private static final Passenger IVAN = new Passenger("PP1", "Іван", LocalDate.of(2000, 5, 12));
    private static final Passenger OLENA = new Passenger("PP4", "Олена", LocalDate.of(2001, 5, 12));

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
    @DisplayName("Loads Valid Records Into Repository")
    void loadsValidRecordsIntoRepository() throws IOException {
        Path file = writeJson("""
                [{"passportSeriesNum":"PP1","fullName":"Іван","birthDate":"2000-05-12"}]
                """);
        PassengerRepository repository = new PassengerRepository();

        LoadResult result = load(file, repository);

        assertEquals(new LoadResult(1, 1, List.of()), result);
        assertEquals(List.of(IVAN), repository.findAll());
    }

    @Test
    @DisplayName("Reports All Validation Errors For Rejected Record")
    void reportsAllValidationErrorsForRejectedRecord() throws IOException {
        Path file = writeJson("""
                [{"passportSeriesNum":"PP2","fullName":""}]
                """);
        PassengerRepository repository = new PassengerRepository();

        LoadResult result = load(file, repository);

        assertEquals(1, result.readCount());
        assertEquals(0, result.addedCount());
        assertEquals(List.of(new LoadResult.Rejection(1, List.of(
                new FieldError("birthDate", "Вкажіть дату народження"),
                new FieldError("fullName", "Вкажіть ім’я пасажира")
        ))), result.rejected());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Continues Loading After Validation Failure")
    void continuesLoadingAfterValidationFailure() throws IOException {
        Path file = writeJson("""
                [
                  {"passportSeriesNum":"PP2","fullName":""},
                  {"passportSeriesNum":"PP4","fullName":"Олена","birthDate":"2001-05-12"}
                ]
                """);
        PassengerRepository repository = new PassengerRepository();

        LoadResult result = load(file, repository);

        assertEquals(2, result.readCount());
        assertEquals(1, result.addedCount());
        assertEquals(List.of(1), result.rejected().stream().map(LoadResult.Rejection::recordNumber).toList());
        assertEquals(List.of(OLENA), repository.findAll());
    }

    @Test
    @DisplayName("Continues Loading After Malformed Record")
    void continuesLoadingAfterMalformedRecord() throws IOException {
        Path file = writeJson("""
                [
                  {"passportSeriesNum":"PP3","fullName":"Марія","birthDate":"bad-date"},
                  {"passportSeriesNum":"PP4","fullName":"Олена","birthDate":"2001-05-12"}
                ]
                """);
        PassengerRepository repository = new PassengerRepository();

        LoadResult result = load(file, repository);

        assertEquals(2, result.readCount());
        assertEquals(1, result.addedCount());
        assertEquals(1, result.rejected().size());
        assertEquals(1, result.rejected().getFirst().recordNumber());
        assertEquals("birthDate", result.rejected().getFirst().errors().getFirst().field());
        assertEquals(List.of(OLENA), repository.findAll());
    }

    @Test
    @DisplayName("Rejects Duplicate Identity Without Replacing Original")
    void rejectsDuplicateIdentityWithoutReplacingOriginal() throws IOException {
        Path file = writeJson("""
                [
                  {"passportSeriesNum":"PP1","fullName":"Іван","birthDate":"2000-05-12"},
                  {"passportSeriesNum":"PP1","fullName":"Петро","birthDate":"1999-05-12"}
                ]
                """);
        PassengerRepository repository = new PassengerRepository();

        LoadResult result = load(file, repository);

        assertEquals(2, result.readCount());
        assertEquals(1, result.addedCount());
        assertEquals(1, result.rejected().size());
        assertEquals(2, result.rejected().getFirst().recordNumber());
        FieldError error = result.rejected().getFirst().errors().getFirst();
        assertEquals("identity", error.field());
        assertTrue(error.message().contains("PP1"));
        assertEquals(List.of(IVAN), repository.findAll());
    }

    @Test
    @DisplayName("Reports Broken Json As File Error Without Changing Repository")
    void reportsBrokenJsonAsFileErrorWithoutChangingRepository() throws IOException {
        Path file = writeJson("""
                [{"passportSeriesNum":"PP1","fullName":"Іван","birthDate":"2000-05-12"},
                """);
        PassengerRepository repository = new PassengerRepository();
        repository.add(OLENA);

        assertFileError(load(file, repository));

        assertEquals(List.of(OLENA), repository.findAll());
    }

    @Test
    @DisplayName("Reports Missing File As File Error")
    void reportsMissingFileAsFileError() {
        assertFileError(load(tempDir.resolve("missing.yaml"), new PassengerRepository()));
    }

    @Test
    @DisplayName("Rejects identity already in repository without replacing it")
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
        LoadResult.Rejection rejection = result.rejected().getFirst();
        assertEquals(1, rejection.recordNumber());
        assertEquals("identity", rejection.errors().getFirst().field());
        assertEquals(List.of(original), repository.findAll());
    }

    private Path writeJson(String content) throws IOException {
        return Files.writeString(tempDir.resolve("passengers.json"), content);
    }

    private LoadResult load(Path file, PassengerRepository repository) {
        return new DataLoader(factory.getValidator())
                .load(file, new FileStorage<>(Passenger.class), repository);
    }

    private static void assertFileError(LoadResult result) {
        assertEquals(0, result.readCount());
        assertEquals(0, result.addedCount());
        assertEquals(1, result.rejected().size());
        var rejection = result.rejected().getFirst();
        assertNull(rejection.recordNumber());
        assertEquals("file", rejection.errors().getFirst().field());
        assertTrue(rejection.errors().getFirst().message().contains("Failed to read file"));
    }
}
