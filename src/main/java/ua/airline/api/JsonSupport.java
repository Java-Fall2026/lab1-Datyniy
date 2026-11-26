package ua.airline.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;

public final class JsonSupport {
    private final ObjectMapper mapper;
    private final Validator validator;

    public JsonSupport(ObjectMapper mapper, Validator validator) {
        this.mapper = Objects.requireNonNull(mapper);
        this.validator = Objects.requireNonNull(validator);
    }

    public <T> T readValid(HttpServletRequest request, Class<T> type) throws IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new ApiException(415, "Expected application/json Content-Type",
                    "Тіло запиту повинно мати Content-Type application/json");
        }
        T item = mapper.readValue(request.getInputStream(), type);
        if (item == null) {
            throw new ApiException(400, "Request body must not be null", "Тіло запиту має бути JSON-об’єктом");
        }
        var violations = validator.validate(item).stream()
                .map(v -> new ApiError.Violation(v.getPropertyPath().toString(), v.getMessage()))
                .sorted(Comparator.comparing(ApiError.Violation::field).thenComparing(ApiError.Violation::message))
                .toList();
        if (!violations.isEmpty()) {
            throw new ApiException(400, "Request body failed validation",
                    "Тіло запиту не пройшло валідацію", violations);
        }
        return item;
    }

    public <T> void write(HttpServletResponse response, int status, T body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");
        response.getOutputStream().write(bytes);
    }
}
