package ua.airline.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.airline.repository.DuplicateIdentityException;

import java.io.IOException;
import java.util.List;

public final class ErrorHandlingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingFilter.class);
    private final JsonSupport json;

    public ErrorHandlingFilter(JsonSupport json) {
        this.json = json;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        try {
            chain.doFilter(request, response);
        } catch (Exception exception) {
            Throwable cause = exception;
            while (cause instanceof ServletException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            ApiError error = errorFor(cause, req.getRequestURI());
            if (error.status() == 500) {
                log.error("Unexpected request failure: {} {}", req.getMethod(), req.getRequestURI(), exception);
            }
            resp.reset();
            json.write(resp, error.status(), error);
        }
    }

    private static ApiError errorFor(Throwable exception, String path) {
        if (exception instanceof ApiException api) {
            return new ApiError(api.status(), statusName(api.status()), api.userMessage(), path, api.violations());
        }
        if (exception instanceof DuplicateIdentityException) {
            return new ApiError(409, "Conflict", "Об’єкт із такою ідентичністю вже існує", path, List.of());
        }
        if (exception instanceof JsonProcessingException) {
            return new ApiError(400, "Bad Request", "Некоректне тіло JSON або тип значення поля", path, List.of());
        }
        return new ApiError(500, "Internal Server Error", "Внутрішня помилка сервера", path, List.of());
    }

    private static String statusName(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 415 -> "Unsupported Media Type";
            default -> "Internal Server Error";
        };
    }
}
