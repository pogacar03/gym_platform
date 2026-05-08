package com.graduation.fitmate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.WorkoutVideoQuery;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationQualityEvaluationTest {

    private final RequestParsingService parser = new RequestParsingService();
    private final RecommendationService recommendationService = new RecommendationService(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );

    @Test
    void shouldMeetPrecisionAndRecallTargetsForCoreDemoScenarios() {
        List<QualityCase> cases = List.of(
                new QualityCase(
                        "chair no equipment",
                        "I don't have equipment and I want to sit on the chair to do exercise for 15 minutes",
                        Set.of(1L, 2L, 9L),
                        false
                ),
                new QualityCase(
                        "unsafe knee hiit",
                        "My knees hurt but I want high intensity jumping HIIT for 20 minutes",
                        Set.of(1L, 4L, 9L, 10L),
                        true
                ),
                new QualityCase(
                        "shoulder recovery",
                        "I have shoulder pain and want a gentle recovery workout",
                        Set.of(7L, 8L),
                        false
                ),
                new QualityCase(
                        "back pain stretch",
                        "My lower back feels stiff, I want a gentle stretch",
                        Set.of(6L),
                        false
                ),
                new QualityCase(
                        "no equipment no jumping cardio",
                        "20 min cardio without equipment and without jumping",
                        Set.of(1L, 4L, 10L),
                        false
                ),
                new QualityCase(
                        "senior gentle seated",
                        "给老人推荐一个10分钟温和的坐姿训练",
                        Set.of(2L, 9L),
                        false
                )
        );

        for (QualityCase qualityCase : cases) {
            Evaluation evaluation = evaluate(qualityCase);

            if (qualityCase.expectedRelevantIds().size() >= 3) {
                assertTrue(
                        evaluation.precisionAt3() >= 0.67d,
                        qualityCase.name() + " precision@3 was " + evaluation.precisionAt3()
                                + " for top ids " + evaluation.topIds()
                );
            } else {
                assertTrue(
                        qualityCase.expectedRelevantIds().contains(evaluation.topIds().get(0)),
                        qualityCase.name() + " top1 was not relevant: " + evaluation.topIds()
                );
            }
            assertTrue(
                    evaluation.recallAt3() >= 0.67d,
                    qualityCase.name() + " recall@3 was " + evaluation.recallAt3()
                            + " for top ids " + evaluation.topIds()
            );
            if (qualityCase.requiresLowImpactSafety()) {
                assertFalse(
                        evaluation.topVideos().stream().anyMatch(video -> "HIGH".equalsIgnoreCase(video.getImpactLevel())),
                        qualityCase.name() + " returned a high-impact video despite safety constraints"
                );
            }
        }
    }

    @Test
    void shouldNormalizeFallbackQueryForCanonicalBodyAreas() {
        UserProfile profile = baseProfile();

        ParsedRecommendationRequest shoulder = parser.parse("shoulder pain recovery", profile);
        WorkoutVideoQuery shoulderQuery = recommendationService.toQuery(shoulder, false);
        assertEquals("ARMS", shoulderQuery.getTargetArea());

        ParsedRecommendationRequest lowerBack = parser.parse("lower back stretch", profile);
        WorkoutVideoQuery lowerBackQuery = recommendationService.toQuery(lowerBack, false);
        assertEquals("BACK", lowerBackQuery.getTargetArea());
    }

    @Test
    void shouldKeepSlightlyLongerRelevantVideosInsideDurationRecallWindow() {
        UserProfile profile = baseProfile();
        ParsedRecommendationRequest parsed = parser.parse("20 min cardio without equipment and without jumping", profile);

        WorkoutVideo twentyFiveMinuteRelevantVideo = video(
                10L,
                "25 Min Low Impact Cardio",
                "Follow along low impact cardio without jumping.",
                "WEIGHT_LOSS",
                "FULL_BODY",
                "NONE",
                "STANDING",
                "LOW",
                25
        );

        assertTrue(recommendationService.matchesSearchConstraints(twentyFiveMinuteRelevantVideo, parsed, false));
        assertEquals(25, recommendationService.toQuery(parsed, false).getMaxDurationMinutes());
    }

    @Test
    void shouldRejectExplainerAndHighImpactTextForSafetySensitiveRecommendations() {
        UserProfile profile = baseProfile();
        ParsedRecommendationRequest parsed =
                parser.parse("My knees hurt but I want high intensity jumping HIIT for 20 minutes", profile);

        WorkoutVideo explainer = video(
                12L,
                "32 Zero Cost Healthy Habits in 20 Minutes",
                "A lifestyle explainer with wellness habits.",
                "WEIGHT_LOSS",
                "FULL_BODY",
                "NONE",
                "STANDING",
                "LOW",
                20
        );
        WorkoutVideo hiitTitle = video(
                13L,
                "20 MIN HIIT Dance Cardio Workout",
                "Fast intervals for cardio.",
                "WEIGHT_LOSS",
                "FULL_BODY",
                "NONE",
                "STANDING",
                "LOW",
                20
        );
        WorkoutVideo noJumping = video(
                14L,
                "20MIN Low Impact Cardio At Home Workout",
                "No jumping follow along workout.",
                "WEIGHT_LOSS",
                "FULL_BODY",
                "NONE",
                "STANDING",
                "LOW",
                20
        );

        assertFalse(recommendationService.matchesSearchConstraints(explainer, parsed, false));
        assertFalse(recommendationService.matchesSearchConstraints(hiitTitle, parsed, false));
        assertTrue(recommendationService.matchesSearchConstraints(noJumping, parsed, false));
    }

    @Test
    void shoulderSensitiveRecoveryShouldAvoidDumbbellLoading() {
        ParsedRecommendationRequest parsed =
                parser.parse("I have shoulder pain and want a gentle recovery workout", baseProfile());
        WorkoutVideo dumbbellShoulder = video(
                15L,
                "8 Minute Arm Workout With Weights",
                "Triceps, biceps, shoulder workout.",
                "RECOVERY",
                "ARMS",
                "DUMBBELL",
                "FLOOR",
                "LOW",
                8
        );
        WorkoutVideo mobility = video(
                16L,
                "Shoulder Mobility Routine",
                "Shoulder pain recovery and rotator cuff mobility exercises.",
                "RECOVERY",
                "ARMS",
                "NONE",
                "STANDING",
                "LOW",
                12
        );

        assertFalse(recommendationService.matchesSearchConstraints(dumbbellShoulder, parsed, false));
        assertTrue(recommendationService.matchesSearchConstraints(mobility, parsed, false));
    }

    @Test
    void backSensitiveStretchShouldStayBackRelevantAndAvoidHiitText() {
        ParsedRecommendationRequest parsed =
                parser.parse("My lower back feels stiff, I want a gentle stretch", baseProfile());
        WorkoutVideo genericLowImpact = video(
                17L,
                "20MIN Low Impact Cardio At Home Workout",
                "No jumping follow along workout.",
                "RECOVERY",
                "FULL_BODY",
                "NONE",
                "FLOOR",
                "LOW",
                20
        );
        WorkoutVideo pilatesHiit = video(
                18L,
                "20 MIN PILATES HIIT",
                "Low impact workout with stretch included.",
                "RECOVERY",
                "FULL_BODY",
                "NONE",
                "FLOOR",
                "LOW",
                20
        );
        WorkoutVideo backStretch = video(
                19L,
                "15 min Stretch and Flow Routine | Back and Hips Focused",
                "Gentle back mobility follow along.",
                "RECOVERY",
                "BACK",
                "NONE",
                "FLOOR",
                "LOW",
                15
        );
        WorkoutVideo dumbbellCombo = video(
                20L,
                "06: Dumbbell Total Body Combo | 32 Mins Workout",
                "A strength-based full-body workout using compound exercises and back-to-basics movements.",
                "RECOVERY",
                "FULL_BODY",
                "DUMBBELL",
                "FLOOR",
                "LOW",
                32
        );

        assertFalse(recommendationService.matchesSearchConstraints(genericLowImpact, parsed, false));
        assertFalse(recommendationService.matchesSearchConstraints(pilatesHiit, parsed, false));
        assertFalse(recommendationService.matchesSearchConstraints(dumbbellCombo, parsed, false));
        assertTrue(recommendationService.matchesSearchConstraints(backStretch, parsed, false));
    }

    @Test
    void fallbackRankingShouldUseTheSameContentQualityGuards() {
        UserProfile profile = baseProfile();
        ParsedRecommendationRequest parsed =
                parser.parse("My knees hurt but I want high intensity jumping HIIT for 20 minutes", profile);

        List<Long> topIds = corpusWithNoisyVideos().stream()
                .filter(video -> video.getDurationMinutes() != null)
                .filter(video -> video.getDurationMinutes() <= parsed.getDurationMinutes() + 10)
                .filter(video -> recommendationService.matchesSearchConstraints(video, parsed, true))
                .sorted(Comparator.comparingInt((WorkoutVideo video) ->
                        recommendationService.scoreVideo(parsed, video, null)).reversed())
                .limit(3)
                .map(WorkoutVideo::getId)
                .toList();

        assertFalse(topIds.contains(12L));
        assertFalse(topIds.contains(13L));
        assertTrue(topIds.stream().anyMatch(Set.of(1L, 4L, 10L, 14L)::contains));
    }

    @Test
    void sparseRetrievalShouldBeSupplementedBySafeSqlCandidates() {
        ParsedRecommendationRequest parsed =
                parser.parse("My knees hurt but I want high intensity jumping HIIT for 20 minutes", baseProfile());

        List<WorkoutVideo> sparseRetrieval = List.of(
                video(14L, "20MIN Low Impact Cardio At Home Workout", "No jumping follow along workout.",
                        "WEIGHT_LOSS", "FULL_BODY", "NONE", "STANDING", "LOW", 20)
        );
        List<WorkoutVideo> sqlCandidates = corpusWithNoisyVideos().stream()
                .filter(video -> recommendationService.matchesSearchConstraints(video, parsed, false))
                .filter(video -> sparseRetrieval.stream().noneMatch(existing -> existing.getId().equals(video.getId())))
                .limit(2)
                .toList();

        List<WorkoutVideo> combined = new java.util.ArrayList<>(sparseRetrieval);
        combined.addAll(sqlCandidates);

        assertEquals(3, combined.size());
        assertFalse(combined.stream().anyMatch(video -> "HIGH".equalsIgnoreCase(video.getImpactLevel())));
        assertFalse(combined.stream().anyMatch(video -> Set.of(12L, 13L).contains(video.getId())));
    }

    private Evaluation evaluate(QualityCase qualityCase) {
        ParsedRecommendationRequest parsed = parser.parse(qualityCase.input(), baseProfile());
        List<WorkoutVideo> topVideos = corpus().stream()
                .filter(video -> recommendationService.matchesSearchConstraints(video, parsed, false))
                .sorted(Comparator.comparingInt((WorkoutVideo video) ->
                        recommendationService.scoreVideo(parsed, video, null)).reversed())
                .limit(3)
                .toList();
        List<Long> topIds = topVideos.stream().map(WorkoutVideo::getId).toList();
        long relevantHits = topIds.stream().filter(qualityCase.expectedRelevantIds()::contains).count();
        double precisionAt3 = topIds.isEmpty() ? 0.0d : relevantHits / (double) topIds.size();
        double recallAt3 = qualityCase.expectedRelevantIds().isEmpty()
                ? 1.0d
                : relevantHits / (double) qualityCase.expectedRelevantIds().size();
        return new Evaluation(topVideos, topIds, precisionAt3, recallAt3);
    }

    private UserProfile baseProfile() {
        UserProfile profile = new UserProfile();
        profile.setFitnessGoal("WEIGHT_LOSS");
        profile.setAvailableEquipment("NONE");
        profile.setPreferredDurationMinutes(20);
        profile.setKneeSensitive(false);
        profile.setBackSensitive(false);
        return profile;
    }

    private List<WorkoutVideo> corpus() {
        return List.of(
                video(1L, "15 Min Seated Low Impact Cardio", "Chair-friendly no jumping follow along workout.",
                        "WEIGHT_LOSS", "FULL_BODY", "NONE", "SITTING", "LOW", 15),
                video(2L, "Chair Assisted Mobility Flow", "Gentle seated mobility routine with chair support.",
                        "RECOVERY", "FULL_BODY", "CHAIR", "SITTING", "LOW", 15),
                video(3L, "Advanced Jumping HIIT", "High impact jumps and burpees.",
                        "WEIGHT_LOSS", "FULL_BODY", "NONE", "STANDING", "HIGH", 20),
                video(4L, "Low Impact Fat Burn Walk", "Standing low impact cardio with no equipment.",
                        "WEIGHT_LOSS", "FULL_BODY", "NONE", "STANDING", "LOW", 20),
                video(5L, "Dumbbell Upper Body Strength", "Upper body strength with dumbbells.",
                        "MUSCLE_TONE", "ARMS", "DUMBBELL", "STANDING", "MEDIUM", 20),
                video(6L, "Gentle Back Stretch", "Lower back mobility and stretching routine.",
                        "RECOVERY", "BACK", "NONE", "FLOOR", "LOW", 18),
                video(7L, "Shoulder Mobility Routine", "Shoulder pain recovery and rotator cuff mobility exercises.",
                        "RECOVERY", "ARMS", "NONE", "STANDING", "LOW", 12),
                video(8L, "Seated Shoulder Rehab", "Chair-supported shoulder recovery exercises.",
                        "RECOVERY", "ARMS", "CHAIR", "SITTING", "LOW", 16),
                video(9L, "Senior Gentle Chair Workout", "Low impact seated exercise for older adults.",
                        "RECOVERY", "FULL_BODY", "CHAIR", "SITTING", "LOW", 10),
                video(10L, "25 Min Low Impact Cardio", "No equipment cardio without jumping.",
                        "WEIGHT_LOSS", "FULL_BODY", "NONE", "STANDING", "LOW", 25),
                video(11L, "Core HIIT Finisher", "Fast core finisher with mountain climbers.",
                        "CORE_STRENGTH", "CORE", "NONE", "FLOOR", "HIGH", 12)
        );
    }

    private List<WorkoutVideo> corpusWithNoisyVideos() {
        List<WorkoutVideo> videos = new java.util.ArrayList<>(corpus());
        videos.add(video(12L, "32 Zero Cost Healthy Habits in 20 Minutes", "A lifestyle explainer.",
                "WEIGHT_LOSS", "FULL_BODY", "NONE", "STANDING", "LOW", 20));
        videos.add(video(13L, "20 MIN HIIT Dance Cardio Workout", "Fast intervals for cardio.",
                "WEIGHT_LOSS", "FULL_BODY", "NONE", "STANDING", "LOW", 20));
        videos.add(video(14L, "20MIN Low Impact Cardio At Home Workout", "No jumping follow along workout.",
                "WEIGHT_LOSS", "FULL_BODY", "NONE", "STANDING", "LOW", 20));
        return videos;
    }

    private WorkoutVideo video(
            Long id,
            String title,
            String description,
            String goal,
            String bodyPart,
            String equipment,
            String posture,
            String impact,
            Integer durationMinutes
    ) {
        WorkoutVideo video = new WorkoutVideo();
        video.setId(id);
        video.setTitle(title);
        video.setDescription(description);
        video.setDifficulty("BEGINNER");
        video.setTargetGoal(goal);
        video.setTargetBodyPart(bodyPart);
        video.setEquipmentRequirement(equipment);
        video.setPostureType(posture);
        video.setImpactLevel(impact);
        video.setDurationMinutes(durationMinutes);
        video.setActive(true);
        video.setCurated(true);
        video.setVideoUrl("https://example.com/video/" + id);
        return video;
    }

    private record QualityCase(
            String name,
            String input,
            Set<Long> expectedRelevantIds,
            boolean requiresLowImpactSafety
    ) {
    }

    private record Evaluation(
            List<WorkoutVideo> topVideos,
            List<Long> topIds,
            double precisionAt3,
            double recallAt3
    ) {
    }
}
