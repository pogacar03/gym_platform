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
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private final UserAccountService userAccountService;
    private final UserProfileService userProfileService;
    private final WorkoutVideoService workoutVideoService;
    private final RequestParsingService requestParsingService;
    private final RecommendationHistoryMapper recommendationHistoryMapper;
    private final WorkoutPlanMapper workoutPlanMapper;
    private final WorkoutPlanItemMapper workoutPlanItemMapper;
    private final LlmGateway llmGateway;

    public RecommendationService(
            UserAccountService userAccountService,
            UserProfileService userProfileService,
            WorkoutVideoService workoutVideoService,
            RequestParsingService requestParsingService,
            RecommendationHistoryMapper recommendationHistoryMapper,
            WorkoutPlanMapper workoutPlanMapper,
            WorkoutPlanItemMapper workoutPlanItemMapper,
            LlmGateway llmGateway
    ) {
        this.userAccountService = userAccountService;
        this.userProfileService = userProfileService;
        this.workoutVideoService = workoutVideoService;
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
        List<WorkoutVideo> candidates = workoutVideoService.findCandidates(query);
        if (candidates.isEmpty()) {
            query.setRelaxGoal(true);
            candidates = workoutVideoService.findCandidates(query);
        }
        if (candidates.isEmpty()) {
            candidates = workoutVideoService.findAllActive().stream().limit(3).toList();
        }
        if (candidates.size() > 3) {
            candidates = candidates.subList(0, 3);
        }

        RecommendationResult result = new RecommendationResult();
        result.setTitle(buildPlanTitle(parsed));
        result.getVideos().addAll(candidates);
        result.setExplanation(llmGateway.generateExplanation(profile, parsed, candidates));
        result.setSafetyNotes(buildSafetyNote(parsed));
        result.setFallbackUsed(true);

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

    private WorkoutVideoQuery toQuery(ParsedRecommendationRequest parsed, boolean relaxGoal) {
        WorkoutVideoQuery query = new WorkoutVideoQuery();
        query.setGoal(parsed.getGoal());
        query.setMaxDurationMinutes(parsed.getDurationMinutes());
        query.setEquipment(parsed.getEquipment());
        query.setPostureType(parsed.getPostureType());
        query.setTargetArea(parsed.getTargetArea());
        query.setKneeSensitive(parsed.isKneeSensitive());
        query.setBackSensitive(parsed.isBackSensitive());
        query.setRelaxGoal(relaxGoal);
        return query;
    }

    private String buildPlanTitle(ParsedRecommendationRequest parsed) {
        return parsed.getDurationMinutes() + "-minute " + humanizeGoal(parsed.getGoal()) + " session";
    }

    private String buildSafetyNote(ParsedRecommendationRequest parsed) {
        if (parsed.getSafetyFlags().isEmpty()) {
            return "No extra safety flags detected. Maintain comfortable intensity and hydrate well.";
        }
        return "Safety filters applied: " + String.join(", ", parsed.getSafetyFlags()) + ". Stop immediately if pain increases.";
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
}
