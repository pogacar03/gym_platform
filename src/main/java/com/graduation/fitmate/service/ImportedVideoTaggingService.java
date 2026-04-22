package com.graduation.fitmate.service;

import com.graduation.fitmate.dto.ImportedVideoSuggestion;
import com.graduation.fitmate.entity.ImportSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ImportedVideoTaggingService {

    private static final Pattern DURATION_PATTERN =
            Pattern.compile("(\\d{1,3})\\s*(分钟|mins?|minutes?)", Pattern.CASE_INSENSITIVE);

    public ImportedVideoSuggestion suggest(ImportSource source, String title, String description) {
        String text = (title + " " + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
        ImportedVideoSuggestion suggestion = new ImportedVideoSuggestion();
        int confidence = 20;

        suggestion.setGoal(firstNonNull(
                inferGoal(text),
                source.getDefaultGoal(),
                "WEIGHT_LOSS"));
        confidence += suggestion.getGoal() != null ? 15 : 0;

        suggestion.setEquipment(firstNonNull(
                inferEquipment(text),
                source.getDefaultEquipment(),
                "NONE"));
        confidence += suggestion.getEquipment() != null ? 10 : 0;

        suggestion.setPosture(firstNonNull(
                inferPosture(text),
                source.getDefaultPosture(),
                "STANDING"));
        confidence += suggestion.getPosture() != null ? 10 : 0;

        suggestion.setTargetArea(firstNonNull(inferTargetArea(text), "FULL_BODY"));
        confidence += !"FULL_BODY".equals(suggestion.getTargetArea()) ? 15 : 5;

        suggestion.setDifficulty(inferDifficulty(text));
        confidence += suggestion.getDifficulty() != null ? 10 : 0;

        suggestion.setImpactLevel(inferImpactLevel(text));
        confidence += suggestion.getImpactLevel() != null ? 10 : 0;

        if ("BEGINNER".equals(suggestion.getDifficulty())) {
            suggestion.getExtraTags().add("BEGINNER_FRIENDLY");
        }
        if ("CHAIR".equals(suggestion.getEquipment())) {
            suggestion.getExtraTags().add("CHAIR_FRIENDLY");
        }
        if ("LOW".equals(suggestion.getImpactLevel())) {
            suggestion.getExtraTags().add("LOW_IMPACT");
        }
        if ("RECOVERY".equals(suggestion.getGoal())) {
            suggestion.getExtraTags().add("RECOVERY_FOCUS");
        }
        if (containsAny(text, "senior", "older adult")) {
            suggestion.getExtraTags().add("SENIOR_FRIENDLY");
        }
        if (containsAny(text, "back pain", "back friendly")) {
            suggestion.getExtraTags().add("BACK_FRIENDLY");
        }
        if (containsAny(text, "knee friendly", "no jumping")) {
            suggestion.getExtraTags().add("KNEE_FRIENDLY");
        }
        if (containsAny(text, "small space", "apartment")) {
            suggestion.getExtraTags().add("SMALL_SPACE");
        }

        if (text.contains("jump") || text.contains("hiit")) {
            suggestion.getSafetyFlags().add("HIGH_IMPACT_REVIEW");
            confidence -= 10;
        }
        if (text.contains("advanced")) {
            suggestion.getSafetyFlags().add("ADVANCED_INTENSITY");
            confidence -= 5;
        }
        if (text.contains("chair") || text.contains("seated")) {
            suggestion.getSafetyFlags().add("CHAIR_FRIENDLY");
        }

        BigDecimal normalized = BigDecimal.valueOf(Math.max(0, Math.min(confidence, 95)))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        suggestion.setConfidenceScore(normalized);
        return suggestion;
    }

    private String inferGoal(String text) {
        if (containsAny(text, "mobility", "recovery", "stretch")) {
            return "RECOVERY";
        }
        if (containsAny(text, "core", "abs", "stability")) {
            return "CORE_STRENGTH";
        }
        if (containsAny(text, "band", "tone", "upper body", "strength")) {
            return "MUSCLE_TONE";
        }
        if (containsAny(text, "fat burn", "walking", "cardio", "hiit")) {
            return "WEIGHT_LOSS";
        }
        return null;
    }

    private String inferEquipment(String text) {
        if (containsAny(text, "chair", "seated")) {
            return "CHAIR";
        }
        if (containsAny(text, "band", "resistance band")) {
            return "BANDS";
        }
        if (containsAny(text, "dumbbell")) {
            return "DUMBBELL";
        }
        if (containsAny(text, "no equipment", "bodyweight", "walking", "low impact")) {
            return "NONE";
        }
        return null;
    }

    private String inferPosture(String text) {
        if (containsAny(text, "chair", "seated", "sitting")) {
            return "SITTING";
        }
        if (containsAny(text, "floor", "mat", "pilates")) {
            return "FLOOR";
        }
        if (containsAny(text, "walk", "standing")) {
            return "STANDING";
        }
        return null;
    }

    private String inferTargetArea(String text) {
        if (containsAny(text, "upper body", "arms", "bicep", "tricep", "shoulder")) {
            return "ARMS";
        }
        if (containsAny(text, "chest")) {
            return "CHEST";
        }
        if (containsAny(text, "back")) {
            return "BACK";
        }
        if (containsAny(text, "core", "abs", "stability")) {
            return "CORE";
        }
        if (containsAny(text, "glute")) {
            return "GLUTES";
        }
        if (containsAny(text, "leg", "lower body")) {
            return "LEGS";
        }
        return null;
    }

    private String inferDifficulty(String text) {
        if (containsAny(text, "beginner", "gentle", "easy")) {
            return "BEGINNER";
        }
        if (containsAny(text, "advanced")) {
            return "ADVANCED";
        }
        if (containsAny(text, "intermediate", "challenge")) {
            return "INTERMEDIATE";
        }
        return "BEGINNER";
    }

    private String inferImpactLevel(String text) {
        if (containsAny(text, "low impact", "chair", "gentle", "mobility")) {
            return "LOW";
        }
        if (containsAny(text, "jump", "hiit", "plyo")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    public Integer inferDurationMinutes(String title, String description) {
        String text = (title == null ? "" : title) + " " + (description == null ? "" : description);
        Matcher matcher = DURATION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        int duration = Integer.parseInt(matcher.group(1));
        if (duration <= 0 || duration > 180) {
            return null;
        }
        return duration;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
