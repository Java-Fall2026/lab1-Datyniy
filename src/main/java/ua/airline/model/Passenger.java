package ua.airline.model;

import ua.airline.util.AirlineUtils;

import java.time.LocalDate;

public record Passenger(
        String passportSeriesNum,
        String fullName,
        LocalDate birthDate
) {
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