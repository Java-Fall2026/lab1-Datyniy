package ua.airline.storage;

import java.util.List;

public record LoadResult(int readCount, int addedCount, List<Rejection> rejected) {
    public LoadResult {
        rejected = List.copyOf(rejected);
    }

    // A null record number indicates a file-level error.
    public record Rejection(Integer recordNumber, List<FieldError> errors) {
        public Rejection {
            errors = List.copyOf(errors);
        }
    }

    public record FieldError(String field, String message) {
    }
}
