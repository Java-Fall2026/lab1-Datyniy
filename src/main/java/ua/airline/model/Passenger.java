package ua.airline.model;

import ua.airline.util.AirlineUtils;

import java.time.LocalDate;
import java.util.Comparator;

public record Passenger(
        String passportSeriesNum,
        String fullName,
        LocalDate birthDate
) implements Comparable<Passenger> {
    private static final Comparator<Passenger> NATURAL_ORDER =
            Comparator.comparing(Passenger::passportSeriesNum)
                    .thenComparing(Passenger::fullName)
                    .thenComparing(Passenger::birthDate);

    public Passenger {
        passportSeriesNum = AirlineUtils.requireNotBlank(
                passportSeriesNum,
                "Passport series and number"
        );

        fullName = AirlineUtils.requireNotBlank(
                fullName,
                "Full name"
        );

        birthDate = AirlineUtils.validateBirthDate(birthDate);
    }

    @Override
    public int compareTo(Passenger other) {
        return NATURAL_ORDER.compare(this, other);
    }

    public static Passenger of(
            String passportSeriesNum,
            String fullName,
            LocalDate birthDate
    ) {
        return new Passenger(
                passportSeriesNum,
                fullName,
                birthDate
        );
    }
}
