package ua.airline.util;

import java.util.Locale;

final class FormatHelper {

    private FormatHelper() {
    }

    static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    static String normalizeLowercase(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    static String capitalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return normalized;
        }

        return normalized.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalized.substring(1);
    }

    static String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}