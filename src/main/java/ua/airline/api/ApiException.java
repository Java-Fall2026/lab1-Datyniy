package ua.airline.api;

import java.util.List;

public final class ApiException extends RuntimeException {
    private final int status;
    private final String userMessage;
    private final List<ApiError.Violation> violations;

    public ApiException(int status, String message, String userMessage) {
        this(status, message, userMessage, List.of());
    }

    public ApiException(int status, String message, String userMessage, List<ApiError.Violation> violations) {
        super(message);
        this.status = status;
        this.userMessage = userMessage;
        this.violations = List.copyOf(violations);
    }

    public int status() {
        return status;
    }

    public String userMessage() {
        return userMessage;
    }

    public List<ApiError.Violation> violations() {
        return violations;
    }

    public static ApiException notFound() {
        return new ApiException(404, "Resource not found", "Ресурс не знайдено");
    }

    public static ApiException methodNotAllowed() {
        return new ApiException(405, "Method not allowed for this path", "Метод не дозволено для цього шляху");
    }

    public static ApiException invalidParameter(String name, String explanation) {
        return new ApiException(400, "Invalid query parameter: " + name,
                "Невалідний параметр " + name + ": " + explanation);
    }
}
