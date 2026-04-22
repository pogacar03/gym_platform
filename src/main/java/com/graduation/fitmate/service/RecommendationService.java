package com.graduation.fitmate.service;

import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.RecommendationResult;
import com.graduation.fitmate.dto.WorkoutVideoQuery;
import com.graduation.fitmate.entity.RecommendationHistory;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutPlan;
import com.graduation.fitmate.entity.WorkoutPlanItem;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.llm.LlmGateway;
import com.graduation.fitmate.mapper.RecommendationHistoryMapper;
import com.graduation.fitmate.mapper.WorkoutPlanItemMapper;
import com.graduation.fitmate.mapper.WorkoutPlanMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private final UserAccountService userAccountService;
    private final UserProfileService userProfileService;
    private final WorkoutVideoService workoutVideoService;
    private final WorkoutVideoSearchService workoutVideoSearchService;
    private final RequestParsingService requestParsingService;
    private final RecommendationHistoryMapper recommendationHistoryMapper;
    private final WorkoutPlanMapper workoutPlanMapper;
    private final WorkoutPlanItemMapper workoutPlanItemMapper;
    private final LlmGateway llmGateway;

    public RecommendationService(
            UserAccountService userAccountService,
            UserProfileService userProfileService,
            WorkoutVideoService workoutVideoService,
            WorkoutVideoSearchService workoutVideoSearchService,
            RequestParsingService requestParsingService,
            RecommendationHistoryMapper recommendationHistoryMapper,
            WorkoutPlanMapper workoutPlanMapper,
            WorkoutPlanItemMapper workoutPlanItemMapper,
            LlmGateway llmGateway
    ) {
        this.userAccountService = userAccountService;
        this.userProfileService = userProfileService;
        this.workoutVideoService = workoutVideoService;
        this.workoutVideoSearchService = workoutVideoSearchService;
        this.requestParsingService = requestParsingService;
        this.recommendationHistoryMapper = recommendationHistoryMapper;
        this.workoutPlanMapper = workoutPlanMapper;
        this.workoutPlanItemMapper = workoutPlanItemMapper;
        this.llmGateway = llmGateway;
    }

    @Transactional
    public RecommendationResult recommend(String username, String requestText) {
        UserAccount account = userAccountService.findByUsername(username);
        UserProfile profile = userProfileService.getProfileByUsername(username);
        if (profile == null) {
            throw new IllegalStateException("Profile is required before requesting recommendations.");
        }

        ParsedRecommendationRequest parsed = requestParsingService.parse(requestText, profile);
        WorkoutVideoQuery query = toQuery(parsed, false);
        List<WorkoutVideo> candidates = findCandidates(parsed, requestText, false, query);
        String fallbackMessage = null;
        if (candidates.isEmpty()) {
            query.setRelaxGoal(true);
            candidates = findCandidates(parsed, requestText, true, query);
            fallbackMessage = "No exact match was found, so the system relaxed the goal filter and kept safety-related conditions.";
        }
        if (candidates.isEmpty()) {
            candidates = fallbackCandidates(parsed);
            fallbackMessage = "No close database match was found, so the system returned the safest broadly relevant options from the curated library.";
        }
        if (candidates.size() > 3) {
            candidates = candidates.subList(0, 3);
        }

        RecommendationResult result = new RecommendationResult();
        result.setTitle(buildPlanTitle(parsed));
        result.setSummary(buildSummary(parsed, candidates));
        result.getVideos().addAll(candidates);
        result.setExplanation(buildExplanation(profile, parsed, candidates));
        result.setSafetyNotes(buildSafetyNote(parsed));
        result.setFallbackUsed(fallbackMessage != null);
        result.setFallbackMessage(fallbackMessage);
        result.getAppliedFilters().addAll(buildAppliedFilters(parsed));
        result.getVideoReasons().putAll(buildVideoReasons(parsed, candidates));

        RecommendationHistory history = new RecommendationHistory();
        history.setUserId(account.getId());
        history.setRequestText(requestText);
        history.setParsedGoal(parsed.getGoal());
        history.setParsedDurationMinutes(parsed.getDurationMinutes());
        history.setParsedEquipment(parsed.getEquipment());
        history.setSafetyFlags(String.join(",", parsed.getSafetyFlags()));
        history.setExplanation(result.getExplanation());
        recommendationHistoryMapper.insert(history);

        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(account.getId());
        plan.setRecommendationId(history.getId());
        plan.setTitle(result.getTitle());
        plan.setSummary(candidates.stream().map(WorkoutVideo::getTitle).collect(Collectors.joining(" -> ")));
        workoutPlanMapper.insert(plan);
        result.setPlanId(plan.getId());

        int order = 1;
        for (WorkoutVideo video : candidates) {
            WorkoutPlanItem item = new WorkoutPlanItem();
            item.setPlanId(plan.getId());
            item.setVideoId(video.getId());
            item.setSortOrder(order++);
            item.setSetsCount(1);
            item.setRepsOrDuration(video.getDurationMinutes() + " min follow-along");
            workoutPlanItemMapper.insert(item);
        }
        return result;
    }

    private List<WorkoutVideo> fallbackCandidates(ParsedRecommendationRequest parsed) {
        return workoutVideoService.findAllActive().stream()
                .filter(video -> parsed.getDurationMinutes() == null || video.getDurationMinutes() == null
                        || video.getDurationMinutes() <= parsed.getDurationMinutes() + 10)
                .filter(video -> !parsed.isKneeSensitive() || !"HIGH".equalsIgnoreCase(video.getImpactLevel()))
                .filter(video -> !parsed.isBackSensitive() || !"CORE".equalsIgnoreCase(video.getTargetBodyPart()))
                .sorted((left, right) -> Integer.compare(scoreVideo(parsed, right), scoreVideo(parsed, left)))
                .limit(3)
                .toList();
    }

    private List<WorkoutVideo> findCandidates(
            ParsedRecommendationRequest parsed,
            String requestText,
            boolean relaxGoal,
            WorkoutVideoQuery query
    ) {
        List<Long> searchIds = workoutVideoSearchService.searchCandidateIds(parsed, requestText, relaxGoal);
        if (!searchIds.isEmpty()) {
            List<WorkoutVideo> indexedCandidates = workoutVideoService.findByIdsPreservingOrder(searchIds).stream()
                    .filter(video -> matchesSearchConstraints(video, parsed, relaxGoal))
                    .toList();
            if (!indexedCandidates.isEmpty()) {
                return indexedCandidates;
            }
        }
        return workoutVideoService.findCandidates(query);
    }

    private boolean matchesSearchConstraints(WorkoutVideo video, ParsedRecommendationRequest parsed, boolean relaxGoal) {
        if (!relaxGoal && parsed.getGoal() != null && !matches(video.getTargetGoal(), parsed.getGoal())) {
            return false;
        }
        if (parsed.getDurationMinutes() != null
                && video.getDurationMinutes() != null
                && video.getDurationMinutes() > parsed.getDurationMinutes()) {
            return false;
        }
        if (parsed.isKneeSensitive() && "HIGH".equalsIgnoreCase(video.getImpactLevel())) {
            return false;
        }
        if (parsed.isBackSensitive() && "CORE".equalsIgnoreCase(video.getTargetBodyPart())) {
            return false;
        }
        if (parsed.getPostureType() != null && !matches(video.getPostureType(), parsed.getPostureType())) {
            return false;
        }
        if (parsed.getTargetArea() != null
                && !matches(video.getTargetBodyPart(), parsed.getTargetArea())
                && !"FULL_BODY".equalsIgnoreCase(video.getTargetBodyPart())) {
            return false;
        }
        if (parsed.getImpactLevel() != null && !matches(video.getImpactLevel(), parsed.getImpactLevel())) {
            return false;
        }
        if (parsed.getEquipment() != null) {
            if ("NONE".equalsIgnoreCase(parsed.getEquipment()) && "SITTING".equalsIgnoreCase(parsed.getPostureType())) {
                return matches(video.getEquipmentRequirement(), "NONE")
                        || matches(video.getEquipmentRequirement(), "CHAIR");
            }
            if ("NONE".equalsIgnoreCase(parsed.getEquipment())) {
                return matches(video.getEquipmentRequirement(), "NONE");
            }
            return matches(video.getEquipmentRequirement(), "NONE")
                    || matches(video.getEquipmentRequirement(), parsed.getEquipment());
        }
        return true;
    }

    private WorkoutVideoQuery toQuery(ParsedRecommendationRequest parsed, boolean relaxGoal) {
        WorkoutVideoQuery query = new WorkoutVideoQuery();
        query.setGoal(parsed.getGoal());
        query.setMaxDurationMinutes(parsed.getDurationMinutes());
        query.setEquipment(parsed.getEquipment());
        query.setPostureType(parsed.getPostureType());
        query.setTargetArea(parsed.getTargetArea());
        query.setImpactLevel(parsed.getImpactLevel());
        query.setKneeSensitive(parsed.isKneeSensitive());
        query.setBackSensitive(parsed.isBackSensitive());
        query.setRelaxGoal(relaxGoal);
        return query;
    }

    private String buildPlanTitle(ParsedRecommendationRequest parsed) {
        return parsed.getDurationMinutes() + "-minute " + humanizeGoal(parsed.getGoal()) + " session";
    }

    private String buildSummary(ParsedRecommendationRequest parsed, List<WorkoutVideo> candidates) {
        String posture = parsed.getPostureType() == null ? "flexible posture" : humanizeEnum(parsed.getPostureType());
        String equipment = parsed.getEquipment() == null ? "available equipment" : humanizeEnum(parsed.getEquipment());
        String area = parsed.getTargetArea() == null ? "full-body support" : humanizeEnum(parsed.getTargetArea());
        String impact = parsed.getImpactLevel() == null ? "a flexible intensity level" : humanizeEnum(parsed.getImpactLevel()) + " impact";
        return "Built around " + parsed.getDurationMinutes() + " minutes, " + posture + ", " + equipment
                + ", " + impact + ", and a focus on " + area + ". " + candidates.size() + " curated videos matched best.";
    }

    private String buildExplanation(UserProfile profile, ParsedRecommendationRequest parsed, List<WorkoutVideo> candidates) {
        try {
            return llmGateway.generateExplanation(profile, parsed, candidates);
        } catch (Exception ex) {
            return "These recommendations were assembled from your profile, requested duration, posture, equipment access, and safety filters.";
        }
    }

    private String buildSafetyNote(ParsedRecommendationRequest parsed) {
        if (parsed.getSafetyFlags().isEmpty()) {
            return "No extra safety flags detected. Maintain comfortable intensity and hydrate well.";
        }
        return "Safety filters applied: " + String.join(", ", parsed.getSafetyFlags()) + ". Stop immediately if pain increases.";
    }

    private List<String> buildAppliedFilters(ParsedRecommendationRequest parsed) {
        return Stream.of(
                        parsed.getEquipment() == null ? null : "Equipment: " + humanizeEnum(parsed.getEquipment()),
                        parsed.getPostureType() == null ? null : "Posture: " + humanizeEnum(parsed.getPostureType()),
                        parsed.getTargetArea() == null ? null : "Target area: " + humanizeEnum(parsed.getTargetArea()),
                        parsed.getImpactLevel() == null ? null : "Impact: " + humanizeEnum(parsed.getImpactLevel()),
                        parsed.getGoal() == null ? null : "Goal: " + humanizeGoal(parsed.getGoal()),
                        parsed.isKneeSensitive() ? "Knee-sensitive filter" : null,
                        parsed.isBackSensitive() ? "Back-friendly filter" : null,
                        parsed.getDurationMinutes() == null ? null : "Duration <= " + parsed.getDurationMinutes() + " min"
                )
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private Map<Long, List<String>> buildVideoReasons(ParsedRecommendationRequest parsed, List<WorkoutVideo> videos) {
        return videos.stream().collect(Collectors.toMap(
                WorkoutVideo::getId,
                video -> {
                    List<String> reasons = new java.util.ArrayList<>();
                    if (matches(video.getEquipmentRequirement(), parsed.getEquipment())) {
                        reasons.add("Equipment fits your setup");
                    }
                    if (matches(video.getPostureType(), parsed.getPostureType())) {
                        reasons.add("Posture matches your request");
                    }
                    if (matches(video.getTargetBodyPart(), parsed.getTargetArea()) || "FULL_BODY".equalsIgnoreCase(video.getTargetBodyPart())) {
                        reasons.add("Body area is aligned");
                    }
                    if (matches(video.getTargetGoal(), parsed.getGoal())) {
                        reasons.add("Training goal is aligned");
                    }
                    if (matches(video.getImpactLevel(), parsed.getImpactLevel())) {
                        reasons.add("Impact level matches your request");
                    }
                    if (parsed.isKneeSensitive() && !"HIGH".equalsIgnoreCase(video.getImpactLevel())) {
                        reasons.add("Lower-impact option");
                    }
                    if (parsed.isBackSensitive() && !"CORE".equalsIgnoreCase(video.getTargetBodyPart())) {
                        reasons.add("Back-friendly choice");
                    }
                    if (reasons.isEmpty()) {
                        reasons.add("Closest safe match from the curated library");
                    }
                    return reasons;
                },
                (left, right) -> left,
                java.util.LinkedHashMap::new
        ));
    }

    private int scoreVideo(ParsedRecommendationRequest parsed, WorkoutVideo video) {
        int score = 0;
        if (matches(video.getEquipmentRequirement(), parsed.getEquipment())) {
            score += 4;
        }
        if (matches(video.getPostureType(), parsed.getPostureType())) {
            score += 4;
        }
        if (matches(video.getTargetGoal(), parsed.getGoal())) {
            score += 3;
        }
        if (matches(video.getImpactLevel(), parsed.getImpactLevel())) {
            score += 3;
        }
        if (matches(video.getTargetBodyPart(), parsed.getTargetArea()) || "FULL_BODY".equalsIgnoreCase(video.getTargetBodyPart())) {
            score += 2;
        }
        if (parsed.isKneeSensitive() && !"HIGH".equalsIgnoreCase(video.getImpactLevel())) {
            score += 2;
        }
        if (parsed.isBackSensitive() && !"CORE".equalsIgnoreCase(video.getTargetBodyPart())) {
            score += 2;
        }
        return score;
    }

    private boolean matches(String value, String expected) {
        if (value == null || expected == null) {
            return false;
        }
        return value.equalsIgnoreCase(expected);
    }

    private String humanizeGoal(String goal) {
        return switch (goal) {
            case "WEIGHT_LOSS" -> "weight loss";
            case "MUSCLE_TONE" -> "muscle tone";
            case "RECOVERY" -> "recovery";
            case "CORE_STRENGTH" -> "core strength";
            default -> goal == null ? "workout" : goal.toLowerCase();
        };
    }

    private String humanizeEnum(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
