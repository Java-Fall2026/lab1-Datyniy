package ua.airline.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Comparator;

public record Passenger(
        @NotBlank(message = "Вкажіть серію та номер паспорта")
        String passportSeriesNum,

        @NotBlank(message = "Вкажіть ім’я пасажира")
        @Size(max = 100, message = "Ім’я пасажира має містити не більше 100 символів")
        String fullName,

        @NotNull(message = "Вкажіть дату народження")
        @PastOrPresent(message = "Дата народження не може бути в майбутньому")
        LocalDate birthDate
) implements Comparable<Passenger> {
    private static final Comparator<Passenger> NATURAL_ORDER =
            Comparator.comparing(Passenger::passportSeriesNum)
                    .thenComparing(Passenger::fullName)
                    .thenComparing(Passenger::birthDate);

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
