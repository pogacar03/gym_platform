package com.graduation.fitmate.service;

import com.graduation.fitmate.dto.ImportedVideoSuggestion;
import com.graduation.fitmate.entity.ImportSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImportedVideoTaggingServiceTest {

    private final ImportedVideoTaggingService service = new ImportedVideoTaggingService();

    @Test
    void shouldInferChairFriendlyMetadata() {
        ImportSource source = new ImportSource();
        source.setDefaultGoal("RECOVERY");
        source.setDefaultEquipment("CHAIR");
        source.setDefaultPosture("SITTING");

        ImportedVideoSuggestion suggestion = service.suggest(
                source,
                "15 Min Chair Cardio Workout for Beginners",
                "Seated low impact workout with no jumping."
        );

        Assertions.assertEquals("CHAIR", suggestion.getEquipment());
        Assertions.assertEquals("SITTING", suggestion.getPosture());
        Assertions.assertEquals("LOW", suggestion.getImpactLevel());
        Assertions.assertTrue(suggestion.getConfidenceScore().doubleValue() >= 0.5);
    }
}
