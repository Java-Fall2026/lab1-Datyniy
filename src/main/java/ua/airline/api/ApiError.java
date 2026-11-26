package ua.airline.api;

import java.util.List;

public record ApiError(int status, String error, String message, String path, List<Violation> violations) {
    public ApiError {
        violations = List.copyOf(violations);
    }

    public record Violation(String field, String message) {
    }
}
