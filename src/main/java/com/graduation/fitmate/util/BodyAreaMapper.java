package com.graduation.fitmate.util;

import java.util.Locale;
import java.util.Map;

public final class BodyAreaMapper {

    private static final Map<String, String> CANONICAL_AREAS = Map.ofEntries(
            Map.entry("SHOULDERS", "ARMS"),
            Map.entry("BICEPS", "ARMS"),
            Map.entry("TRICEPS", "ARMS"),
            Map.entry("FOREARMS", "ARMS"),
            Map.entry("CHEST", "CHEST"),
            Map.entry("ABS", "CORE"),
            Map.entry("OBLIQUES", "CORE"),
            Map.entry("CORE", "CORE"),
            Map.entry("TRAPS", "BACK"),
            Map.entry("LATS", "BACK"),
            Map.entry("UPPER_BACK", "BACK"),
            Map.entry("LOWER_BACK", "BACK"),
            Map.entry("BACK", "BACK"),
            Map.entry("GLUTES", "GLUTES"),
            Map.entry("QUADS", "LEGS"),
            Map.entry("HAMSTRINGS", "LEGS"),
            Map.entry("CALVES", "LEGS"),
            Map.entry("KNEES", "LEGS"),
            Map.entry("LEGS", "LEGS"),
            Map.entry("FULL_BODY", "FULL_BODY")
    );

    private BodyAreaMapper() {
    }

    public static String toCanonical(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = normalize(value);
        return CANONICAL_AREAS.getOrDefault(normalized, normalized);
    }

    public static boolean sameArea(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return toCanonical(left).equalsIgnoreCase(toCanonical(right));
    }

    private static String normalize(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
