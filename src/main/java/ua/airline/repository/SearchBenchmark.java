package ua.airline.repository;

import ua.airline.model.Passenger;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Compares repository lookup with linear search on the same passenger data.
 * This educational benchmark includes loop and result-consumption overhead.
 */
public final class SearchBenchmark {

    private static final int ITEM_COUNT = 100_000;
    private static final int WARMUP_SEARCHES = 2_000;
    private static final int SAMPLES = 9;
    private static final int SEARCHES_PER_SAMPLE = 500;

    private static volatile int sink;

    private SearchBenchmark() {
    }

    public static void main(String[] args) {
        IdentityExtractor<Passenger> extractor =
                passenger -> passenger.passportSeriesNum();
        GenericRepository<Passenger> repository = new GenericRepository<>(
                "benchmark-passengers",
                extractor
        );
        LocalDate birthDate = LocalDate.of(2000, 1, 1);

        for (int i = 0; i < ITEM_COUNT; i++) {
            repository.add(Passenger.of(
                    "BENCH-" + i,
                    "Passenger " + i,
                    birthDate
            ));
        }

        // Take the snapshot once, before warm-up and measurements.
        List<Passenger> items = repository.findAll();
        List<String> identities = List.of(
                "BENCH-0",
                "BENCH-" + (ITEM_COUNT - 1),
                "BENCH-MISSING"
        );
        List<String> scenarios = List.of("First", "Last", "Missing");

        System.out.println("Items: " + ITEM_COUNT);
        System.out.println("Warm-up searches per method and scenario: " + WARMUP_SEARCHES);
        System.out.println("Samples: " + SAMPLES);
        System.out.println("Searches per sample: " + SEARCHES_PER_SAMPLE);
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("VM: " + System.getProperty("java.vm.name"));
        System.out.println(
                "OS: " + System.getProperty("os.name")
                        + " " + System.getProperty("os.arch")
        );
        System.out.println();

        // Warm up both paths for every scenario before collecting samples.
        for (String identity : identities) {
            warmUp(
                    () -> repository.findByIdentity(identity),
                    () -> linearSearch(items, identity, extractor)
            );
        }

        System.out.println("| Scenario | Identity | Map, ns/search | List, ns/search |");
        System.out.println("|---|---|---:|---:|");

        for (int scenario = 0; scenario < identities.size(); scenario++) {
            String identity = identities.get(scenario);
            Supplier<Optional<Passenger>> mapSearch =
                    () -> repository.findByIdentity(identity);
            Supplier<Optional<Passenger>> listSearch =
                    () -> linearSearch(items, identity, extractor);
            long[] mapSamples = new long[SAMPLES];
            long[] listSamples = new long[SAMPLES];

            // Alternate measurement order to reduce a fixed-order bias.
            for (int sample = 0; sample < SAMPLES; sample++) {
                if (sample % 2 == 0) {
                    mapSamples[sample] = measureBatch(mapSearch);
                    listSamples[sample] = measureBatch(listSearch);
                } else {
                    listSamples[sample] = measureBatch(listSearch);
                    mapSamples[sample] = measureBatch(mapSearch);
                }
            }

            System.out.printf(
                    Locale.ROOT,
                    "| %s | %s | %.1f | %.1f |%n",
                    scenarios.get(scenario),
                    identity,
                    medianPerSearch(mapSamples),
                    medianPerSearch(listSamples)
            );
        }

        System.out.println("Last batch matches: " + sink);
    }

    private static <E> Optional<E> linearSearch(
            List<E> items,
            String identity,
            IdentityExtractor<E> extractor
    ) {
        for (E item : items) {
            if (identity.equals(extractor.identityOf(item))) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private static void warmUp(
            Supplier<Optional<Passenger>> mapSearch,
            Supplier<Optional<Passenger>> listSearch
    ) {
        int found = 0;
        for (int search = 0; search < WARMUP_SEARCHES; search++) {
            if (mapSearch.get().isPresent()) {
                found++;
            }
            if (listSearch.get().isPresent()) {
                found++;
            }
        }
        sink = found;
    }

    private static long measureBatch(Supplier<Optional<Passenger>> search) {
        int found = 0;
        long start = System.nanoTime();
        for (int iteration = 0; iteration < SEARCHES_PER_SAMPLE; iteration++) {
            if (search.get().isPresent()) {
                found++;
            }
        }
        long elapsed = System.nanoTime() - start;
        // Consume results outside the timed batch to prevent dead-code removal.
        sink = found;
        return elapsed;
    }

    private static double medianPerSearch(long[] samples) {
        Arrays.sort(samples);
        return (double) samples[samples.length / 2] / SEARCHES_PER_SAMPLE;
    }
}
