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

    @Test
    void shouldParseImpactLevelFromStructuredPhrases() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("RECOVERY");
        profile.setAvailableEquipment("NONE");
        profile.setPreferredDurationMinutes(20);

        ParsedRecommendationRequest parsed = service.parse("20 minutes, no equipment, low impact, back workout", profile);

        Assertions.assertEquals("LOW", parsed.getImpactLevel());
        Assertions.assertEquals("UPPER_BACK", parsed.getTargetArea());
    }

    @Test
    void shouldParseShoulderPainAsRecoveryRequest() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("WEIGHT_LOSS");
        profile.setAvailableEquipment("NONE");
        profile.setPreferredDurationMinutes(20);

        ParsedRecommendationRequest parsed = service.parse("我有一点肩周炎，想缓解一下，怎么训练？", profile);

        Assertions.assertEquals("RECOVERY", parsed.getGoal());
        Assertions.assertEquals("SHOULDERS", parsed.getTargetArea());
        Assertions.assertTrue(parsed.isShoulderSensitive());
        Assertions.assertTrue(parsed.isExplicitGoal());
        Assertions.assertTrue(parsed.isExplicitTargetArea());
        Assertions.assertFalse(parsed.isExplicitEquipment());
    }

    @Test
    void shouldParseChairNoEquipmentRequestIntoStructuredFilters() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("WEIGHT_LOSS");
        profile.setAvailableEquipment("DUMBBELL");
        profile.setPreferredDurationMinutes(20);

        ParsedRecommendationRequest parsed =
                service.parse("I don't have equipment and I want to sit on the chair to do exercise for 15 minutes", profile);

        Assertions.assertEquals("NONE", parsed.getEquipment());
        Assertions.assertEquals("SITTING", parsed.getPostureType());
        Assertions.assertEquals(15, parsed.getDurationMinutes());
        Assertions.assertTrue(parsed.isExplicitEquipment());
        Assertions.assertTrue(parsed.isExplicitPosture());
        Assertions.assertTrue(parsed.getSafetyFlags().contains("SITTING_REQUIRED"));
    }

    @Test
    void safetySignalsShouldOverrideUnsafeHighImpactRequest() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("WEIGHT_LOSS");
        profile.setAvailableEquipment("NONE");
        profile.setPreferredDurationMinutes(20);

        ParsedRecommendationRequest parsed =
                service.parse("My knees hurt but I want a high intensity jumping HIIT workout for 30 minutes", profile);

        Assertions.assertTrue(parsed.isKneeSensitive());
        Assertions.assertEquals("LOW", parsed.getImpactLevel());
        Assertions.assertEquals(30, parsed.getDurationMinutes());
        Assertions.assertTrue(parsed.getSafetyFlags().contains("KNEE_SENSITIVE"));
    }

    @Test
    void shouldTreatNoJumpingAsLowImpact() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("WEIGHT_LOSS");
        profile.setAvailableEquipment("NONE");
        profile.setPreferredDurationMinutes(20);

        ParsedRecommendationRequest parsed =
                service.parse("20 min cardio without equipment and without jumping", profile);

        Assertions.assertEquals("WEIGHT_LOSS", parsed.getGoal());
        Assertions.assertEquals("NONE", parsed.getEquipment());
        Assertions.assertEquals("LOW", parsed.getImpactLevel());
    }

    @Test
    void shouldParseSeniorSeatedRequestAsGentleRecovery() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("WEIGHT_LOSS");
        profile.setAvailableEquipment("NONE");
        profile.setPreferredDurationMinutes(20);

        ParsedRecommendationRequest parsed = service.parse("给老人推荐一个10分钟温和的坐姿训练", profile);

        Assertions.assertEquals("RECOVERY", parsed.getGoal());
        Assertions.assertEquals(10, parsed.getDurationMinutes());
        Assertions.assertEquals("SITTING", parsed.getPostureType());
        Assertions.assertEquals("LOW", parsed.getImpactLevel());
    }
}
