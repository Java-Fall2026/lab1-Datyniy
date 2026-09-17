package ua.airline.util;

final class ValidationHelper {

    private ValidationHelper() {
    }

    static <T> T requireNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null, got: null"
            );
        }
        return value;
    }

    static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank, got: " + value
            );
        }
        return value;
    }

    static double requirePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than 0, got: " + value
            );
        }
        return value;
    }

    static int requireInRange(
            int value,
            int min,
            int max,
            String fieldName
    ) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    fieldName + " must be between "
                            + min + " and " + max
                            + ", got: " + value
            );
        }
        return value;
    }
}