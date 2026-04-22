package com.graduation.fitmate.service;

import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.entity.UserProfile;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RequestParsingService {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d{1,3})\\s*(分钟|min|minutes?)", Pattern.CASE_INSENSITIVE);

    public ParsedRecommendationRequest parse(String requestText, UserProfile profile) {
        ParsedRecommendationRequest parsed = new ParsedRecommendationRequest();
        String text = requestText == null ? "" : requestText.toLowerCase(Locale.ROOT);

        parsed.setGoal(detectGoal(text, profile));
        parsed.setDurationMinutes(detectDuration(text, profile));
        parsed.setEquipment(detectEquipment(text, profile));
        parsed.setPostureType(detectPosture(text, profile));
        parsed.setTargetArea(detectTargetArea(text, profile));
        parsed.setImpactLevel(detectImpactLevel(text));
        parsed.setKneeSensitive(Boolean.TRUE.equals(profile.getKneeSensitive()) || containsAny(text, "膝", "knee"));
        parsed.setBackSensitive(Boolean.TRUE.equals(profile.getBackSensitive()) || containsAny(text, "腰", "back"));

        if (parsed.isKneeSensitive()) {
            parsed.getSafetyFlags().add("KNEE_SENSITIVE");
        }
        if (parsed.isBackSensitive()) {
            parsed.getSafetyFlags().add("BACK_SENSITIVE");
        }
        if ("SITTING".equals(parsed.getPostureType())) {
            parsed.getSafetyFlags().add("SITTING_REQUIRED");
        }
        return parsed;
    }

    private String detectGoal(String text, UserProfile profile) {
        if (containsAny(text, "减脂", "燃脂", "fat", "weight loss", "slim")) {
            return "WEIGHT_LOSS";
        }
        if (containsAny(text, "增肌", "肌肉", "muscle", "tone")) {
            return "MUSCLE_TONE";
        }
        if (containsAny(text, "恢复", "mobility", "recovery")) {
            return "RECOVERY";
        }
        if (containsAny(text, "核心", "core")) {
            return "CORE_STRENGTH";
        }
        return profile.getFitnessGoal() == null ? "WEIGHT_LOSS" : profile.getFitnessGoal();
    }

    private Integer detectDuration(String text, UserProfile profile) {
        Matcher matcher = DURATION_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return profile.getPreferredDurationMinutes() == null ? 20 : profile.getPreferredDurationMinutes();
    }

    private String detectEquipment(String text, UserProfile profile) {
        if (containsAny(text, "无器械", "徒手", "bodyweight", "no equipment")) {
            return "NONE";
        }
        if (containsAny(text, "弹力带", "band")) {
            return "BANDS";
        }
        if (containsAny(text, "椅子", "chair")) {
            return "CHAIR";
        }
        if (containsAny(text, "哑铃", "dumbbell")) {
            return "DUMBBELL";
        }
        return firstValue(profile.getAvailableEquipment(), "NONE");
    }

    private String detectPosture(String text, UserProfile profile) {
        if (containsAny(text, "坐着", "坐姿", "chair", "seated", "sitting")) {
            return "SITTING";
        }
        if (containsAny(text, "站着", "站姿", "standing")) {
            return "STANDING";
        }
        if (containsAny(text, "地面", "垫上", "floor", "mat")) {
            return "FLOOR";
        }
        return profile.getPosturePreference() == null ? null : profile.getPosturePreference();
    }

    private String detectTargetArea(String text, UserProfile profile) {
        if (containsAny(text, "手臂", "arm", "arms", "bicep", "tricep")) {
            return "ARMS";
        }
        if (containsAny(text, "胸", "chest")) {
            return "CHEST";
        }
        if (containsAny(text, "背", "back")) {
            return "BACK";
        }
        if (containsAny(text, "核心", "腹", "core", "abs")) {
            return "CORE";
        }
        if (containsAny(text, "臀", "glute", "glutes")) {
            return "GLUTES";
        }
        if (containsAny(text, "腿", "leg", "legs", "thigh")) {
            return "LEGS";
        }
        return firstValue(profile.getTargetAreas(), null);
    }

    private String detectImpactLevel(String text) {
        if (containsAny(text, "低冲击", "low impact", "gentle")) {
            return "LOW";
        }
        if (containsAny(text, "高强度", "high intensity", "hiit", "高冲击", "high impact")) {
            return "HIGH";
        }
        if (containsAny(text, "中等强度", "moderate intensity", "medium impact", "moderate")) {
            return "MEDIUM";
        }
        return null;
    }

    private String firstValue(String csv, String defaultValue) {
        if (csv == null || csv.isBlank()) {
            return defaultValue;
        }
        return csv.split(",")[0].trim();
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
