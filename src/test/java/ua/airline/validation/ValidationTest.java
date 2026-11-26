package ua.airline.validation;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import ua.airline.model.Airport;
import ua.airline.model.Flight;
import ua.airline.model.FlightStatus;
import ua.airline.model.Passenger;
import ua.airline.storage.LoadResult.FieldError;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    private static final LocalDate BIRTH_DATE = LocalDate.of(2000, 5, 12);
    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 11, 2, 23, 30);
    private static final Airport FROM = new Airport("KBP", "Kyiv");
    private static final Airport TO = new Airport("WAW", "Warsaw");
    private static ValidatorFactory factory;

    @BeforeAll
    static void startValidation() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void stopValidation() {
        factory.close();
    }

    @ParameterizedTest(name = "{0}: constraint {index}")
    @DisplayName("Reports Standard Constraint With Ukrainian Message")
    @MethodSource("invalidStandardValues")
    void reportsStandardConstraintWithUkrainianMessage(
            String field, Object item, Class<? extends Annotation> annotation, String message) {
        var violations = factory.getValidator().validate(item);

        assertEquals(1, violations.size());
        var violation = violations.iterator().next();
        assertEquals(annotation, violation.getConstraintDescriptor().getAnnotation().annotationType());
        assertEquals(field, violation.getPropertyPath().toString());
        assertEquals(message, violation.getMessage());
    }

    static Stream<Arguments> invalidStandardValues() {
        var cases = Stream.<Arguments>builder();
        for (String blank : new String[]{null, "", " "}) {
            cases.add(Arguments.of("passportSeriesNum", new Passenger(blank, "Ivan", BIRTH_DATE),
                    NotBlank.class, "Вкажіть серію та номер паспорта"));
            cases.add(Arguments.of("fullName", new Passenger("PP1", blank, BIRTH_DATE),
                    NotBlank.class, "Вкажіть ім’я пасажира"));
        }
        cases.add(Arguments.of("fullName", new Passenger("PP1", "A".repeat(101), BIRTH_DATE),
                Size.class, "Ім’я пасажира має містити не більше 100 символів"));
        cases.add(Arguments.of("birthDate", new Passenger("PP1", "Ivan", null),
                NotNull.class, "Вкажіть дату народження"));
        cases.add(Arguments.of("birthDate", new Passenger("PP1", "Ivan", LocalDate.now().plusDays(1)),
                PastOrPresent.class, "Дата народження не може бути в майбутньому"));
        for (String blank : new String[]{null, ""}) {
            cases.add(Arguments.of("flightNumber", flight(blank, FROM, TO, DEPARTURE,
                    DEPARTURE.plusHours(1), FlightStatus.SCHEDULED), NotBlank.class, "Вкажіть номер рейсу"));
        }
        cases.add(Arguments.of("flightNumber", flight("PS/101", FROM, TO, DEPARTURE,
                DEPARTURE.plusHours(1), FlightStatus.SCHEDULED), Pattern.class,
                "Номер рейсу може містити лише латинські літери, цифри та дефіс"));
        cases.add(Arguments.of("departureAirport", flight("PS-101", null, TO, DEPARTURE,
                DEPARTURE.plusHours(1), FlightStatus.SCHEDULED), NotNull.class, "Вкажіть аеропорт вильоту"));
        cases.add(Arguments.of("arrivalAirport", flight("PS-101", FROM, null, DEPARTURE,
                DEPARTURE.plusHours(1), FlightStatus.SCHEDULED), NotNull.class, "Вкажіть аеропорт прибуття"));
        cases.add(Arguments.of("departureTime", flight("PS-101", FROM, TO, null,
                DEPARTURE.plusHours(1), FlightStatus.SCHEDULED), NotNull.class, "Вкажіть час вильоту"));
        cases.add(Arguments.of("arrivalTime", flight("PS-101", FROM, TO, DEPARTURE,
                null, FlightStatus.SCHEDULED), NotNull.class, "Вкажіть час прибуття"));
        cases.add(Arguments.of("status", flight("PS-101", FROM, TO, DEPARTURE,
                DEPARTURE.plusHours(1), null), NotNull.class, "Вкажіть статус рейсу"));
        return cases.build();
    }

    @ParameterizedTest(name = "Valid standard constraints: {index}")
    @DisplayName("Accepts Valid Standard Values")
    @MethodSource("validStandardValues")
    void acceptsValidStandardValues(Object item) {
        assertTrue(factory.getValidator().validate(item).isEmpty());
    }

    static Stream<Object> validStandardValues() {
        return Stream.of(
                new Passenger("PP1", "Ivan", BIRTH_DATE),
                new Passenger("PP1", "A".repeat(100), BIRTH_DATE),
                new Passenger("PP1", "Ivan", LocalDate.now()),
                flight("PS-101", FROM, TO, DEPARTURE, DEPARTURE.plusMinutes(1), FlightStatus.SCHEDULED));
    }

    @ParameterizedTest(name = "Arrival offset in minutes: {0}")
    @DisplayName("Rejects Arrival Not Strictly After Departure")
    @ValueSource(ints = {-1, 0})
    void rejectsArrivalNotStrictlyAfterDeparture(int offset) {
        Flight flight = flight("PS-101", FROM, TO, DEPARTURE,
                DEPARTURE.plusMinutes(offset), FlightStatus.SCHEDULED);

        var violations = factory.getValidator().validate(flight);

        assertEquals(1, violations.size());
        var violation = violations.iterator().next();
        assertEquals(ValidFlightTimes.class,
                violation.getConstraintDescriptor().getAnnotation().annotationType());
        assertEquals("arrivalTime", violation.getPropertyPath().toString());
        assertEquals("Час прибуття має бути пізнішим за час вильоту", violation.getMessage());
    }

    @Test
    @DisplayName("Accepts Arrival After Departure Across Midnight")
    void acceptsArrivalAfterDepartureAcrossMidnight() {
        Flight flight = flight("PS-101", FROM, TO, DEPARTURE,
                DEPARTURE.plusHours(1), FlightStatus.SCHEDULED);

        assertTrue(factory.getValidator().validate(flight).isEmpty());
    }

    @Test
    @DisplayName("Returns All Passenger Constraint Violations In One Validation")
    void returnsAllPassengerConstraintViolationsInOneValidation() {
        Passenger passenger = new Passenger("", "A".repeat(101), LocalDate.now().plusDays(1));

        var errors = factory.getValidator().validate(passenger).stream()
                .map(violation -> new FieldError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                new FieldError("passportSeriesNum", "Вкажіть серію та номер паспорта"),
                new FieldError("fullName", "Ім’я пасажира має містити не більше 100 символів"),
                new FieldError("birthDate", "Дата народження не може бути в майбутньому")
        ), errors);
    }

    private static Flight flight(String number, Airport from, Airport to,
                                 LocalDateTime departure, LocalDateTime arrival, FlightStatus status) {
        return new Flight(number, from, to, departure, arrival, status);
    }
}
