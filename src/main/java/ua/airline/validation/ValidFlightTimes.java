package ua.airline.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = FlightTimesValidator.class)
public @interface ValidFlightTimes {
    String message() default "Час прибуття має бути пізнішим за час вильоту";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
