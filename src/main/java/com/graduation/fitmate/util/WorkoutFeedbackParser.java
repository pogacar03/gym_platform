package com.graduation.fitmate.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WorkoutFeedbackParser {

    private static final String PREFIX = "USER_FEEDBACK:";

    private WorkoutFeedbackParser() {
    }

    public static String code(String note) {
        if (note == null || !note.startsWith(PREFIX)) {
            return null;
        }
        String payload = note.substring(PREFIX.length());
        int separator = payload.indexOf('|');
        return separator >= 0 ? payload.substring(0, separator) : payload;
    }

    public static Map<String, String> metadata(String note) {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (note == null || !note.startsWith(PREFIX)) {
            return metadata;
        }
        String payload = note.substring(PREFIX.length());
        String[] parts = payload.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            int separator = parts[i].indexOf('=');
            if (separator <= 0) {
                continue;
            }
            metadata.put(parts[i].substring(0, separator), parts[i].substring(separator + 1));
        }
        return metadata;
    }

    public static String build(String code, Integer actualMinutes, Integer skippedItems, String discomfortArea, String note) {
        StringBuilder builder = new StringBuilder(PREFIX).append(isBlank(code) ? "JUST_RIGHT" : code);
        append(builder, "actual", actualMinutes == null ? null : String.valueOf(Math.max(0, actualMinutes)));
        append(builder, "skipped", skippedItems == null ? null : String.valueOf(Math.max(0, skippedItems)));
        append(builder, "discomfort", discomfortArea);
        append(builder, "note", note);
        return builder.toString();
    }

    private static void append(StringBuilder builder, String key, String value) {
        if (isBlank(value)) {
            return;
        }
        builder.append('|').append(key).append('=').append(sanitize(value));
    }

    private static String sanitize(String value) {
        return value.trim()
                .replace("|", " ")
                .replace("=", ":")
                .replaceAll("\\s+", " ");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
