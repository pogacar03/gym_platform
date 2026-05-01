package com.graduation.fitmate.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.RecommendationKnowledgeNote;
import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecommendationKnowledgeServiceTest {

    private RecommendationKnowledgeService recommendationKnowledgeService;

    @BeforeEach
    void setUp() {
        recommendationKnowledgeService = new RecommendationKnowledgeService(new ObjectMapper());
    }

    @Test
    void shouldPrioritizeChairAndKneeNotesForSeatedLowImpactRequest() {
        ParsedRecommendationRequest parsed = new ParsedRecommendationRequest();
        parsed.setPostureType("SITTING");
        parsed.setEquipment("CHAIR");
        parsed.setImpactLevel("LOW");
        parsed.setKneeSensitive(true);
        parsed.setDurationMinutes(15);

        WorkoutVideo video = new WorkoutVideo();
        video.setDifficulty("BEGINNER");
        video.setExtraTags("BEGINNER_FRIENDLY,CHAIR_FRIENDLY,LOW_IMPACT");

        List<RecommendationKnowledgeNote> notes = recommendationKnowledgeService.selectNotes(
                parsed,
                List.of(video),
                Locale.ENGLISH
        );

        assertThat(notes).isNotEmpty();
        assertThat(notes.get(0).getTitle()).containsAnyOf("chair", "knees", "low-impact", "warm-up");
        assertThat(notes.stream().map(RecommendationKnowledgeNote::getTitle))
                .anyMatch(title -> title.toLowerCase(Locale.ROOT).contains("chair")
                        || title.toLowerCase(Locale.ROOT).contains("knee")
                        || title.toLowerCase(Locale.ROOT).contains("low-impact"));
    }

    @Test
    void shouldReturnChineseKnowledgeForChineseLocale() {
        ParsedRecommendationRequest parsed = new ParsedRecommendationRequest();
        parsed.setGoal("RECOVERY");
        parsed.setBackSensitive(true);
        parsed.setTargetArea("BACK");
        parsed.setDurationMinutes(20);

        List<RecommendationKnowledgeNote> notes = recommendationKnowledgeService.selectNotes(
                parsed,
                List.of(),
                Locale.SIMPLIFIED_CHINESE
        );

        assertThat(notes).isNotEmpty();
        assertThat(notes.get(0).getTitle()).containsAnyOf("恢复", "背部", "热身");
    }
}
