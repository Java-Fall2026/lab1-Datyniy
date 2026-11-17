package ua.airline.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ua.airline.model.Flight;

public class FlightTimesValidator implements ConstraintValidator<ValidFlightTimes, Flight> {
    @Override
    public boolean isValid(Flight flight, ConstraintValidatorContext context) {
        if (flight == null || flight.getDepartureTime() == null || flight.getArrivalTime() == null) {
            return true;
        }
        if (flight.getArrivalTime().isAfter(flight.getDepartureTime())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("arrivalTime")
                .addConstraintViolation();
        return false;
    }
}
