package com.graduation.fitmate.service;

import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.entity.UserProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RequestParsingServiceTest {

    private final RequestParsingService service = new RequestParsingService();

    @Test
    void shouldParseChineseRequestWithKneeConstraint() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("WEIGHT_LOSS");
        profile.setAvailableEquipment("NONE");
        profile.setPreferredDurationMinutes(20);
        profile.setKneeSensitive(false);

        ParsedRecommendationRequest parsed =
                service.parse("我想减脂，只有20分钟，无器械，而且膝盖不太舒服", profile);

        Assertions.assertEquals("WEIGHT_LOSS", parsed.getGoal());
        Assertions.assertEquals(20, parsed.getDurationMinutes());
        Assertions.assertEquals("NONE", parsed.getEquipment());
        Assertions.assertTrue(parsed.isKneeSensitive());
    }

    @Test
    void shouldFallbackToProfileDefaultsWhenRequestIsGeneric() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("RECOVERY");
        profile.setAvailableEquipment("CHAIR");
        profile.setPreferredDurationMinutes(15);
        profile.setKneeSensitive(true);

        ParsedRecommendationRequest parsed = service.parse("give me something easy", profile);

        Assertions.assertEquals("RECOVERY", parsed.getGoal());
        Assertions.assertEquals(15, parsed.getDurationMinutes());
        Assertions.assertEquals("CHAIR", parsed.getEquipment());
        Assertions.assertTrue(parsed.isKneeSensitive());
    }
}
