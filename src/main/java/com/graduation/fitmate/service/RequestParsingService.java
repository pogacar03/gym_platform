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

        parsed.setExplicitGoal(hasGoalSignal(text));
        parsed.setExplicitDuration(DURATION_PATTERN.matcher(text).find());
        parsed.setExplicitEquipment(hasEquipmentSignal(text));
        parsed.setExplicitPosture(hasPostureSignal(text));
        parsed.setExplicitTargetArea(hasTargetAreaSignal(text));
        parsed.setExplicitImpactLevel(hasImpactSignal(text));

        parsed.setGoal(detectGoal(text, profile));
        parsed.setDurationMinutes(detectDuration(text, profile));
        parsed.setEquipment(detectEquipment(text, profile));
        parsed.setPostureType(detectPosture(text, profile));
        parsed.setTargetArea(detectTargetArea(text, profile));
        parsed.setKneeSensitive(Boolean.TRUE.equals(profile.getKneeSensitive()) || containsAny(text, "膝", "knee"));
        parsed.setBackSensitive(Boolean.TRUE.equals(profile.getBackSensitive()) || containsAny(text, "腰", "back"));
        String injuryNotes = profile.getInjuryNotes() == null ? "" : profile.getInjuryNotes().toLowerCase(Locale.ROOT);
        parsed.setShoulderSensitive(
                containsAny(text, "肩周炎", "肩痛", "肩膀痛", "肩不舒服", "肩", "shoulder pain", "frozen shoulder", "rotator cuff")
                        || containsAny(injuryNotes, "肩周炎", "肩痛", "肩膀痛", "肩不舒服", "肩", "shoulder pain", "frozen shoulder", "rotator cuff")
        );
        parsed.setImpactLevel(detectImpactLevel(text, parsed));

        if (parsed.isKneeSensitive()) {
            parsed.getSafetyFlags().add("KNEE_SENSITIVE");
        }
        if (parsed.isBackSensitive()) {
            parsed.getSafetyFlags().add("BACK_SENSITIVE");
        }
        if (parsed.isShoulderSensitive()) {
            parsed.getSafetyFlags().add("SHOULDER_SENSITIVE");
        }
        if ("SITTING".equals(parsed.getPostureType())) {
            parsed.getSafetyFlags().add("SITTING_REQUIRED");
        }
        return parsed;
    }

    private String detectGoal(String text, UserProfile profile) {
        if (containsAny(text, "减脂", "燃脂", "瘦身", "fat", "weight loss", "lose weight", "slim", "cardio")) {
            return "WEIGHT_LOSS";
        }
        if (containsAny(text, "增肌", "肌肉", "muscle", "tone")) {
            return "MUSCLE_TONE";
        }
        if (containsAny(text, "恢复", "康复", "缓解", "疼", "痛", "不舒服", "僵硬", "拉伸", "放松", "老人", "长者",
                "mobility", "recovery", "rehab", "pain", "stiff", "relief", "frozen shoulder", "rotator cuff")) {
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
        if (containsAny(text, "无器械", "不用器械", "徒手", "bodyweight", "no equipment", "without equipment",
                "without any equipment", "don't have equipment", "do not have equipment", "no weights")) {
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
        if (containsAny(text, "肩周炎", "肩膀", "肩部", "肩", "shoulder", "shoulders", "delts", "rotator cuff", "frozen shoulder")) {
            return "SHOULDERS";
        }
        if (containsAny(text, "二头", "肱二头", "bicep", "biceps")) {
            return "BICEPS";
        }
        if (containsAny(text, "三头", "肱三头", "tricep", "triceps")) {
            return "TRICEPS";
        }
        if (containsAny(text, "前臂", "小臂", "forearm", "forearms")) {
            return "FOREARMS";
        }
        if (containsAny(text, "手臂", "arm", "arms", "bicep", "tricep")) {
            return "ARMS";
        }
        if (containsAny(text, "胸", "chest")) {
            return "CHEST";
        }
        if (containsAny(text, "斜方", "traps", "trapezius")) {
            return "TRAPS";
        }
        if (containsAny(text, "背阔", "lat", "lats", "latissimus")) {
            return "LATS";
        }
        if (containsAny(text, "上背", "upper back")) {
            return "UPPER_BACK";
        }
        if (containsAny(text, "腰", "下背", "lower back", "low back")) {
            return "LOWER_BACK";
        }
        if (containsAny(text, "背", "back")) {
            return "UPPER_BACK";
        }
        if (containsAny(text, "侧腹", "腹斜", "oblique", "obliques")) {
            return "OBLIQUES";
        }
        if (containsAny(text, "腹肌", "腹部", "腹", "abs", "abdominal")) {
            return "ABS";
        }
        if (containsAny(text, "核心", "core")) {
            return "CORE";
        }
        if (containsAny(text, "臀", "glute", "glutes")) {
            return "GLUTES";
        }
        if (containsAny(text, "股四头", "大腿前侧", "quad", "quads", "quadriceps")) {
            return "QUADS";
        }
        if (containsAny(text, "腘绳", "大腿后侧", "hamstring", "hamstrings")) {
            return "HAMSTRINGS";
        }
        if (containsAny(text, "小腿", "calf", "calves")) {
            return "CALVES";
        }
        if (containsAny(text, "膝", "膝盖", "knee", "knees")) {
            return "KNEES";
        }
        if (containsAny(text, "腿", "leg", "legs", "thigh")) {
            return "LEGS";
        }
        return normalizeProfileTarget(firstValue(profile.getTargetAreas(), null));
    }

    private String detectImpactLevel(String text, ParsedRecommendationRequest parsed) {
        if (parsed.isKneeSensitive() || parsed.isBackSensitive() || parsed.isShoulderSensitive()
                || containsAny(text, "低冲击", "温和", "缓解", "疼", "痛", "不舒服", "僵硬", "康复", "不要跳", "不跳",
                "老人", "长者", "low impact", "gentle", "pain", "stiff", "relief", "rehab", "senior", "elderly",
                "older adult", "no jumping", "without jumping")) {
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

    private boolean hasGoalSignal(String text) {
        return containsAny(text, "减脂", "燃脂", "瘦身", "增肌", "肌肉", "塑形", "恢复", "康复", "缓解", "疼", "痛", "不舒服", "僵硬", "拉伸", "放松", "老人", "长者",
                "fat", "weight loss", "lose weight", "slim", "cardio", "muscle", "tone", "mobility", "recovery", "rehab", "pain", "stiff", "relief", "senior", "elderly", "older adult");
    }

    private boolean hasEquipmentSignal(String text) {
        return containsAny(text, "无器械", "不用器械", "徒手", "弹力带", "椅子", "哑铃", "bodyweight", "no equipment",
                "without equipment", "without any equipment", "don't have equipment", "do not have equipment",
                "no weights", "band", "chair", "dumbbell");
    }

    private boolean hasPostureSignal(String text) {
        return containsAny(text, "坐着", "坐姿", "站着", "站姿", "地面", "垫上", "chair", "seated", "sitting", "standing", "floor", "mat");
    }

    private boolean hasTargetAreaSignal(String text) {
        return containsAny(text, "肩周炎", "肩膀", "肩部", "肩", "手臂", "二头", "三头", "前臂", "胸", "背", "上背", "下背", "腰",
                "核心", "腹", "侧腹", "臀", "腿", "股四头", "腘绳", "小腿", "膝",
                "shoulder", "rotator cuff", "frozen shoulder", "upper body", "arm", "bicep", "tricep", "forearm",
                "chest", "back", "upper back", "lower back", "core", "abs", "oblique", "glute", "leg", "quad",
                "hamstring", "calf", "knee");
    }

    private boolean hasImpactSignal(String text) {
        return containsAny(text, "低冲击", "温和", "高强度", "高冲击", "中等强度",
                "low impact", "gentle", "no jumping", "without jumping", "high intensity", "hiit", "high impact", "medium impact", "moderate");
    }

    private String firstValue(String csv, String defaultValue) {
        if (csv == null || csv.isBlank()) {
            return defaultValue;
        }
        return csv.split(",")[0].trim();
    }

    private String normalizeProfileTarget(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
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
