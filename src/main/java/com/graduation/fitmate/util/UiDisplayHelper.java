package com.graduation.fitmate.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.context.i18n.LocaleContextHolder;

@Component("ui")
public class UiDisplayHelper {

    private final MessageSource messageSource;

    public UiDisplayHelper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String label(String value) {
        if (value == null || value.isBlank()) {
            return message("ui.tagNeeded", "Tag needed");
        }
        String key = "label." + value.toLowerCase(Locale.ROOT);
        String translated = messageSource.getMessage(key, null, null, LocaleContextHolder.getLocale());
        if (translated != null) {
            return translated;
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Arrays.stream(normalized.split(" "))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    public List<String> csvLabels(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of(message("ui.tagNeeded", "Tag needed"));
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::label)
                .toList();
    }

    public List<String> optionalCsvLabels(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::label)
                .toList();
    }

    public String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public String progressStyle(int percent, String color) {
        return "--progress:" + Math.max(0, Math.min(percent, 100)) + ";--accent:" + color + ";";
    }

    public String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        Locale locale = LocaleContextHolder.getLocale();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", locale);
        return value.format(formatter);
    }

    public String message(String key, String fallback) {
        return messageSource.getMessage(key, null, fallback, LocaleContextHolder.getLocale());
    }
}
