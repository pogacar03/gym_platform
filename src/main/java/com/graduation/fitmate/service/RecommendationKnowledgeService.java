package com.graduation.fitmate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.RecommendationKnowledgeNote;
import com.graduation.fitmate.entity.WorkoutVideo;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class RecommendationKnowledgeService {

    private final Map<String, List<KnowledgeSnippet>> snippetsByLocale;

    public RecommendationKnowledgeService(ObjectMapper objectMapper) {
        this.snippetsByLocale = loadSnippets(objectMapper);
    }

    public List<RecommendationKnowledgeNote> selectNotes(
            ParsedRecommendationRequest parsed,
            List<WorkoutVideo> candidates,
            Locale locale
    ) {
        List<KnowledgeSnippet> snippets = snippetsByLocale.getOrDefault(localeKey(locale), snippetsByLocale.get("en"));
        Set<String> contextTags = buildContextTags(parsed, candidates);
        List<RecommendationKnowledgeNote> notes = new ArrayList<>();

        for (KnowledgeSnippet snippet : snippets) {
            int score = scoreSnippet(snippet, parsed, contextTags);
            if (score <= 0) {
                continue;
            }

            RecommendationKnowledgeNote note = new RecommendationKnowledgeNote();
            note.setId(snippet.id());
            note.setTitle(snippet.title());
            note.setBody(snippet.body());
            note.setScore(score);
            notes.add(note);
        }

        return notes.stream()
                .sorted(Comparator.comparingInt(RecommendationKnowledgeNote::getScore).reversed()
                        .thenComparing(RecommendationKnowledgeNote::getTitle))
                .limit(3)
                .toList();
    }

    private Map<String, List<KnowledgeSnippet>> loadSnippets(ObjectMapper objectMapper) {
        Map<String, List<KnowledgeSnippet>> map = new HashMap<>();
        map.put("en", readSnippets(objectMapper, "knowledge/recommendation-knowledge-en.json"));
        map.put("zh", readSnippets(objectMapper, "knowledge/recommendation-knowledge-zh_CN.json"));
        return map;
    }

    private List<KnowledgeSnippet> readSnippets(ObjectMapper objectMapper, String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<KnowledgeSnippet>>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load recommendation knowledge snippets from " + path, ex);
        }
    }

    private String localeKey(Locale locale) {
        if (locale != null && locale.getLanguage().toLowerCase(Locale.ROOT).startsWith("zh")) {
            return "zh";
        }
        return "en";
    }

    private Set<String> buildContextTags(ParsedRecommendationRequest parsed, List<WorkoutVideo> candidates) {
        Set<String> tags = new LinkedHashSet<>();
        if (parsed.getGoal() != null) {
            tags.add("GOAL_" + parsed.getGoal());
        }
        if (parsed.getPostureType() != null) {
            tags.add("POSTURE_" + parsed.getPostureType());
        }
        if (parsed.getEquipment() != null) {
            tags.add("EQUIPMENT_" + parsed.getEquipment());
        }
        if (parsed.getTargetArea() != null) {
            tags.add("TARGET_" + parsed.getTargetArea());
        }
        if (parsed.getImpactLevel() != null) {
            tags.add("IMPACT_" + parsed.getImpactLevel());
        }
        if (parsed.getDurationMinutes() != null && parsed.getDurationMinutes() <= 15) {
            tags.add("DURATION_SHORT");
        }
        if (parsed.isKneeSensitive()) {
            tags.add("KNEE_SENSITIVE");
        }
        if (parsed.isBackSensitive()) {
            tags.add("BACK_SENSITIVE");
        }

        for (WorkoutVideo video : candidates) {
            if (video.getDifficulty() != null) {
                tags.add("DIFFICULTY_" + video.getDifficulty());
            }
            if (video.getExtraTags() != null && !video.getExtraTags().isBlank()) {
                tags.addAll(List.of(video.getExtraTags().split(",")));
            }
        }
        return tags;
    }

    private int scoreSnippet(KnowledgeSnippet snippet, ParsedRecommendationRequest parsed, Set<String> contextTags) {
        int score = 0;
        for (String tag : snippet.tags()) {
            if ("GENERAL".equals(tag)) {
                score += 1;
                continue;
            }
            if (contextTags.contains(tag)) {
                score += 4;
            }
            if ("IMPACT_LOW".equals(tag) && parsed.isKneeSensitive()) {
                score += 2;
            }
            if ("TARGET_BACK".equals(tag) && parsed.isBackSensitive()) {
                score += 2;
            }
            if ("DIFFICULTY_BEGINNER".equals(tag) && contextTags.contains("BEGINNER_FRIENDLY")) {
                score += 2;
            }
            if ("KNEE_SENSITIVE".equals(tag) && parsed.isKneeSensitive()) {
                score += 3;
            }
            if ("BACK_SENSITIVE".equals(tag) && parsed.isBackSensitive()) {
                score += 3;
            }
            if ("POSTURE_SITTING".equals(tag) && "SITTING".equalsIgnoreCase(parsed.getPostureType())) {
                score += 2;
            }
            if ("EQUIPMENT_CHAIR".equals(tag) && "CHAIR".equalsIgnoreCase(parsed.getEquipment())) {
                score += 2;
            }
            if ("EQUIPMENT_NONE".equals(tag) && "NONE".equalsIgnoreCase(parsed.getEquipment())) {
                score += 2;
            }
            if ("GOAL_RECOVERY".equals(tag) && "RECOVERY".equalsIgnoreCase(parsed.getGoal())) {
                score += 2;
            }
        }
        return score;
    }

    public String summarizeNotes(List<RecommendationKnowledgeNote> notes) {
        if (notes.isEmpty()) {
            return "";
        }
        return notes.stream()
                .map(RecommendationKnowledgeNote::getTitle)
                .collect(Collectors.joining(" | "));
    }

    private record KnowledgeSnippet(String id, String title, String body, List<String> tags) {
    }
}
