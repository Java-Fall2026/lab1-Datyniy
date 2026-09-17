package ua.airline.model;

import ua.airline.util.AirlineUtils;
import ua.common.BaseEntity;

import java.time.LocalDate;
import java.util.Objects;

public class Passenger extends BaseEntity {

    private final String passportSeriesNum;
    private final String fullName;
    private final LocalDate birthDate;

    private Passenger(
            String passportSeriesNum,
            String fullName,
            LocalDate birthDate
    ) {
        super();

        this.passportSeriesNum = AirlineUtils.requireNotBlank(
                passportSeriesNum,
                "Passport series and number"
        );

        this.fullName = AirlineUtils.requireNotBlank(
                fullName,
                "Full name"
        );

        this.birthDate = AirlineUtils.validateBirthDate(birthDate);
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

    public String getPassportSeriesNum() {
        return passportSeriesNum;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Passenger passenger = (Passenger) o;

        return Objects.equals(
                passportSeriesNum,
                passenger.passportSeriesNum
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(passportSeriesNum);
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "passportSeriesNum='" + passportSeriesNum + '\'' +
                ", fullName='" + fullName + '\'' +
                ", birthDate=" + birthDate +
                ", createdAt=" + createdAt +
                '}';
    }
}