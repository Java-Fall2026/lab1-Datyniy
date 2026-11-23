package ua.airline.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.airline.repository.DuplicateIdentityException;
import ua.airline.repository.GenericRepository;
import ua.airline.storage.LoadResult.FieldError;
import ua.airline.storage.LoadResult.Rejection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class DataLoader {
    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final Validator validator;

    public DataLoader(Validator validator) {
        this.validator = validator;
    }

    public <T> LoadResult load(Path file, FileStorage<T> storage, GenericRepository<T> repository) {
        ArrayNode records;
        try {
            records = storage.readRecords(file);
        } catch (IOException | IllegalArgumentException e) {
            return report(file, new LoadResult(0, 0, List.of(
                    rejection(null, "file", "Не вдалося прочитати файл. " + e.getMessage()))));
        }

        int added = 0;
        List<Rejection> rejected = new ArrayList<>();
        for (int index = 0; index < records.size(); index++) {
            int number = index + 1;
            T item;
            try {
                item = storage.readItem(file, records.get(index));
            } catch (JsonProcessingException e) {
                rejected.add(rejection(number, mappingField(e),
                        "Не вдалося розібрати запис. " + e.getOriginalMessage()));
                continue;
            }
            if (item == null) {
                rejected.add(rejection(number, "record", "Запис не може бути порожнім"));
                continue;
            }

            List<FieldError> errors = validator.validate(item).stream()
                    .map(violation -> {
                        String field = violation.getPropertyPath().toString();
                        return new FieldError(field.isEmpty() ? "record" : field, violation.getMessage());
                    })
                    .sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
                    .toList();
            if (!errors.isEmpty()) {
                rejected.add(new Rejection(number, errors));
                continue;
            }

            try {
                repository.add(item);
                added++;
            } catch (DuplicateIdentityException e) {
                rejected.add(rejection(number, "identity", "Дублікат ідентичності. " + e.getMessage()));
            }
        }

        return report(file, new LoadResult(records.size(), added, rejected));
    }

    private static String mappingField(JsonProcessingException exception) {
        if (exception instanceof JsonMappingException mapping) {
            String path = mapping.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(field -> field != null)
                    .collect(Collectors.joining("."));
            if (!path.isEmpty()) {
                return path;
            }
        }
        return "record";
    }

    private static Rejection rejection(Integer number, String field, String message) {
        return new Rejection(number, List.of(new FieldError(field, message)));
    }

    private static LoadResult report(Path file, LoadResult result) {
        log.info("{} | прочитано {}, додано {}, відкинуто {}",
                file, result.readCount(), result.addedCount(), result.rejected().size());
        for (Rejection rejection : result.rejected()) {
            String location = rejection.recordNumber() == null
                    ? "помилка файлу" : "запис №" + rejection.recordNumber();
            String details = rejection.errors().stream()
                    .map(error -> error.field() + " — " + error.message().replaceAll("\\R", " "))
                    .collect(Collectors.joining(" | "));
            if (rejection.recordNumber() == null) {
                log.error("{} | {} | {}", file, location, details);
            } else {
                log.warn("{} | {} | {}", file, location, details);
            }
        }
        return result;
    }
}
