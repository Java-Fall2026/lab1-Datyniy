package ua.airline.api;

import jakarta.servlet.http.HttpServletRequest;
import ua.airline.repository.GenericRepository;
import ua.airline.repository.SortOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class QueryParameters {
    private final Map<String, String[]> values;

    QueryParameters(HttpServletRequest request, Set<String> allowed) {
        values = request.getParameterMap();
        for (var entry : values.entrySet()) {
            if (!allowed.contains(entry.getKey())) {
                throw ApiException.invalidParameter(entry.getKey(), "невідомий параметр");
            }
            if (entry.getValue().length != 1) {
                throw ApiException.invalidParameter(entry.getKey(), "параметр не можна повторювати");
            }
            if (entry.getValue()[0].isBlank()) {
                throw ApiException.invalidParameter(entry.getKey(), "значення не може бути порожнім");
            }
        }
    }

    String text(String name) {
        String[] value = values.get(name);
        return value == null ? null : value[0];
    }

    LocalDate date(String name, LocalDate fallback) {
        String value = text(name);
        if (value == null) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidParameter(name, "очікується дата у форматі yyyy-MM-dd");
        }
    }

    LocalDateTime dateTime(String name, LocalDateTime fallback) {
        String value = text(name);
        if (value == null) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidParameter(name, "очікується дата й час у форматі ISO, наприклад 2026-12-01T09:00:00");
        }
    }

    boolean hasRange(String from, String to) {
        return values.containsKey(from) || values.containsKey(to);
    }

    <T> List<T> finish(GenericRepository<T> repository, List<T> selected,
                       Map<String, Comparator<T>> comparators, String defaultSort) {
        String sort = text("sort") == null ? defaultSort : text("sort");
        Comparator<T> comparator = comparators.get(sort);
        if (comparator == null) {
            throw ApiException.invalidParameter("sort", "дозволені значення: " + comparators.keySet());
        }
        String orderText = text("order") == null ? "ASC" : text("order");
        if (!orderText.equals("ASC") && !orderText.equals("DESC")) {
            throw ApiException.invalidParameter("order", "дозволені значення ASC або DESC");
        }
        SortOrder order = SortOrder.valueOf(orderText);
        if (order == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        int limit = nonNegativeInt("limit", Integer.MAX_VALUE);
        int offset = nonNegativeInt("offset", 0);
        Set<T> matches = new HashSet<>(selected);
        return repository.findAllSorted(comparator).stream()
                .filter(matches::contains)
                .skip(offset)
                .limit(limit)
                .toList();
    }

    private int nonNegativeInt(String name, int fallback) {
        String value = text(name);
        if (value == null) {
            return fallback;
        }
        try {
            int number = Integer.parseInt(value);
            if (number < 0) {
                throw ApiException.invalidParameter(name, "очікується ціле число від 0 до 2147483647");
            }
            return number;
        } catch (NumberFormatException e) {
            throw ApiException.invalidParameter(name, "очікується ціле число від 0 до 2147483647");
        }
    }
}
