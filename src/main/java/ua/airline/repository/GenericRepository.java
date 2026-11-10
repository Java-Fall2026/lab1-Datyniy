package ua.airline.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Stores items by an extracted identity in insertion order.
 * Identity fields must remain unchanged while items are stored.
 */
public class GenericRepository<T> {

    private static final Logger log =
            LoggerFactory.getLogger(GenericRepository.class);

    private final String name;
    private final IdentityExtractor<T> extractor;
    private final Map<String, T> items = new LinkedHashMap<>();

    public GenericRepository(String name, IdentityExtractor<T> extractor) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Repository name must not be blank"
            );
        }
        if (extractor == null) {
            throw new IllegalArgumentException(
                    "Identity extractor must not be null"
            );
        }

        this.name = name;
        this.extractor = extractor;
    }

    public void add(T item) {
        String identity = extractValidIdentity(item);

        if (items.containsKey(identity)) {
            log.warn(
                    "Duplicate identity '{}' in repository '{}'",
                    identity,
                    name
            );
            throw new DuplicateIdentityException(identity, name);
        }

        items.put(identity, item);
        log.info("Added identity '{}' to repository '{}'", identity, name);
    }

    public boolean addIfAbsent(T item) {
        String identity = extractValidIdentity(item);

        if (items.containsKey(identity)) {
            log.warn(
                    "Skipped duplicate identity '{}' in repository '{}'",
                    identity,
                    name
            );
            return false;
        }

        items.put(identity, item);
        log.info("Added identity '{}' to repository '{}'", identity, name);
        return true;
    }

    /**
     * Adds items in iteration order and stops at the first invalid or duplicate
     * item. Items added before the failure remain in this repository.
     */
    public void addAll(Collection<? extends T> newItems) {
        if (newItems == null) {
            IllegalArgumentException exception = new IllegalArgumentException(
                    "Collection must not be null"
            );
            log.error("Invalid collection for repository '{}'", name, exception);
            throw exception;
        }

        for (T item : newItems) {
            add(item);
        }
    }

    public boolean removeByIdentity(String identity) {
        T removed = items.remove(identity);

        if (removed == null) {
            log.warn(
                    "Cannot remove missing identity '{}' from repository '{}'",
                    identity,
                    name
            );
            return false;
        }

        log.info("Removed identity '{}' from repository '{}'", identity, name);
        return true;
    }

    public Optional<T> findByIdentity(String identity) {
        T item = items.get(identity);

        if (item == null) {
            log.warn(
                    "Identity '{}' was not found in repository '{}': 0 items",
                    identity,
                    name
            );
            return Optional.empty();
        }

        log.info("Found identity '{}' in repository '{}': 1 item", identity, name);
        return Optional.of(item);
    }

    /**
     * Returns an unmodifiable snapshot of the collection, not a deep copy
     * of the stored items.
     */
    public List<T> findAll() {
        return List.copyOf(items.values());
    }

    public List<T> findAllSorted(Comparator<T> comparator) {
        Objects.requireNonNull(comparator, "Comparator must not be null");
        List<T> result = stream().sorted(comparator).toList();
        log.info("Sorted repository '{}' using comparator '{}': {} items",
                name, comparator, result.size());
        return result;
    }

    /**
     * Sorts string identities lexicographically, not numerically.
     */
    public List<T> findAllSortedByIdentity(SortOrder order) {
        Objects.requireNonNull(order, "Sort order must not be null");
        Comparator<Map.Entry<String, T>> comparator = Map.Entry.comparingByKey();
        if (order == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        List<T> result = items.entrySet().stream()
                .sorted(comparator)
                .map(Map.Entry::getValue)
                .toList();
        log.info("Sorted repository '{}' by identity {}: {} items",
                name, order, result.size());
        return result;
    }

    /**
     * Returns a fresh, single-use stream backed by the repository.
     * Do not structurally modify the repository while consuming it.
     * Unlike findAll(), this method does not create a list snapshot.
     */
    public Stream<T> stream() {
        return items.values().stream();
    }

    public int size() {
        return items.size();
    }

    public static <E> void copyAll(
            GenericRepository<? extends E> source,
            GenericRepository<? super E> target
    ) {
        for (E item : source.findAll()) {
            target.add(item);
        }
    }

    private String extractValidIdentity(T item) {
        if (item == null) {
            IllegalArgumentException exception = new IllegalArgumentException(
                    "Item must not be null"
            );
            log.error("Invalid item for repository '{}'", name, exception);
            throw exception;
        }

        String identity = extractor.identityOf(item);

        if (identity == null || identity.isBlank()) {
            IllegalArgumentException exception = new IllegalArgumentException(
                    "Identity must not be blank"
            );
            log.error("Invalid identity for repository '{}'", name, exception);
            throw exception;
        }

        return identity;
    }
}
