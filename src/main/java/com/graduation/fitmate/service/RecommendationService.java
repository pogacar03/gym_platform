package com.graduation.fitmate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.CandidateLookupResult;
import com.graduation.fitmate.dto.KnowledgeChunkHit;
import com.graduation.fitmate.dto.RecommendationKnowledgeNote;
import com.graduation.fitmate.dto.RecommendationResult;
import com.graduation.fitmate.dto.SearchCandidateScore;
import com.graduation.fitmate.dto.SearchRetrievalResult;
import com.graduation.fitmate.dto.WorkoutVideoQuery;
import com.graduation.fitmate.entity.RecommendationHistory;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutLog;
import com.graduation.fitmate.entity.WorkoutPlan;
import com.graduation.fitmate.entity.WorkoutPlanItem;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.llm.LlmGateway;
import com.graduation.fitmate.mapper.RecommendationHistoryMapper;
import com.graduation.fitmate.mapper.WorkoutLogMapper;
import com.graduation.fitmate.mapper.WorkoutPlanItemMapper;
import com.graduation.fitmate.mapper.WorkoutPlanMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.i18n.LocaleContextHolder;

@Service
public class RecommendationService {

    private final UserAccountService userAccountService;
    private final UserProfileService userProfileService;
    private final WorkoutVideoService workoutVideoService;
    private final WorkoutVideoSearchService workoutVideoSearchService;
    private final RequestParsingService requestParsingService;
    private final RecommendationKnowledgeService recommendationKnowledgeService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final RecommendationHistoryMapper recommendationHistoryMapper;
    private final WorkoutLogMapper workoutLogMapper;
    private final WorkoutPlanMapper workoutPlanMapper;
    private final WorkoutPlanItemMapper workoutPlanItemMapper;
    private final LlmGateway llmGateway;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public RecommendationService(
            UserAccountService userAccountService,
            UserProfileService userProfileService,
            WorkoutVideoService workoutVideoService,
            WorkoutVideoSearchService workoutVideoSearchService,
            RequestParsingService requestParsingService,
            RecommendationKnowledgeService recommendationKnowledgeService,
            KnowledgeSearchService knowledgeSearchService,
            RecommendationHistoryMapper recommendationHistoryMapper,
            WorkoutLogMapper workoutLogMapper,
            WorkoutPlanMapper workoutPlanMapper,
            WorkoutPlanItemMapper workoutPlanItemMapper,
            LlmGateway llmGateway,
            MessageSource messageSource,
            ObjectMapper objectMapper
    ) {
        this.userAccountService = userAccountService;
        this.userProfileService = userProfileService;
        this.workoutVideoService = workoutVideoService;
        this.workoutVideoSearchService = workoutVideoSearchService;
        this.requestParsingService = requestParsingService;
        this.recommendationKnowledgeService = recommendationKnowledgeService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.recommendationHistoryMapper = recommendationHistoryMapper;
        this.workoutLogMapper = workoutLogMapper;
        this.workoutPlanMapper = workoutPlanMapper;
        this.workoutPlanItemMapper = workoutPlanItemMapper;
        this.llmGateway = llmGateway;
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecommendationResult recommend(String username, String requestText) {
        UserAccount account = userAccountService.findByUsername(username);
        UserProfile profile = userProfileService.getProfileByUsername(username);
        if (profile == null) {
            throw new IllegalStateException("Profile is required before requesting recommendations.");
        }

        ParsedRecommendationRequest parsed = requestParsingService.parse(requestText, profile);
        String latestFeedbackCode = latestFeedbackCode(account.getId());
        String feedbackAdjustment = applyRecentFeedbackAdjustment(parsed, latestFeedbackCode);
        WorkoutVideoQuery query = toQuery(parsed, false);
        CandidateLookupResult lookupResult = findCandidates(parsed, requestText, false, query);
        List<WorkoutVideo> candidates = rankCandidates(parsed, lookupResult.getVideos(), latestFeedbackCode);
        String fallbackMessage = null;
        boolean relaxedGoal = false;
        if (candidates.isEmpty()) {
            query.setRelaxGoal(true);
            lookupResult = findCandidates(parsed, requestText, true, query);
            candidates = rankCandidates(parsed, lookupResult.getVideos(), latestFeedbackCode);
            fallbackMessage = fallbackMessage("GOAL_RELAXED");
            relaxedGoal = true;
        }
        if (candidates.isEmpty()) {
            candidates = fallbackCandidates(parsed);
            fallbackMessage = fallbackMessage("SAFE_LIBRARY");
            lookupResult = CandidateLookupResult.builder()
                    .videos(candidates)
                    .retrieval(SearchRetrievalResult.empty())
                    .sqlFallbackUsed(true)
                    .build();
        }
        if (candidates.size() > 3) {
            candidates = candidates.subList(0, 3);
        }
        List<RecommendationKnowledgeNote> knowledgeNotes = resolveKnowledgeNotes(parsed, candidates);

        RecommendationResult result = new RecommendationResult();
        result.setTitle(buildPlanTitle(parsed));
        result.setSummary(buildSummary(parsed, candidates));
        result.getVideos().addAll(candidates);
        result.getKnowledgeNotes().addAll(knowledgeNotes);
        result.setExplanation(buildExplanation(profile, parsed, candidates, knowledgeNotes));
        result.setSafetyNotes(buildSafetyNote(parsed, knowledgeNotes));
        result.setRetrievalSummary(buildRetrievalSummary(lookupResult, relaxedGoal));
        result.setCandidateSummary(buildCandidateSummary(lookupResult, candidates, knowledgeNotes));
        result.setEvidenceSummary(buildEvidenceSummary(lookupResult, knowledgeNotes));
        result.setFallbackUsed(fallbackMessage != null);
        result.setFallbackMessage(fallbackMessage);
        result.setFeedbackAdjustment(feedbackAdjustment);
        result.getAppliedFilters().addAll(buildAppliedFilters(parsed));
        result.getFitReasons().addAll(buildFitReasons(parsed, candidates, latestFeedbackCode));
        result.getCautionItems().addAll(buildCautionItems(parsed, knowledgeNotes));
        result.getKnowledgeReferences().addAll(buildKnowledgeReferences(knowledgeNotes));
        result.getFilteredOutReasons().addAll(buildFilteredOutReasons(parsed, relaxedGoal, lookupResult));
        result.getVideoReasons().putAll(buildVideoReasons(parsed, candidates, latestFeedbackCode));
        result.getScoreBreakdown().putAll(buildScoreBreakdown(parsed, candidates, lookupResult));

        RecommendationHistory history = new RecommendationHistory();
        history.setUserId(account.getId());
        history.setRequestText(requestText);
        history.setParsedGoal(parsed.getGoal());
        history.setParsedDurationMinutes(parsed.getDurationMinutes());
        history.setParsedEquipment(parsed.getEquipment());
        history.setSafetyFlags(String.join(",", parsed.getSafetyFlags()));
        history.setExplanation(result.getExplanation());
        history.setEvidenceJson(toEvidenceJson(result));
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

    private String buildCandidateSummary(
            CandidateLookupResult lookupResult,
            List<WorkoutVideo> candidates,
            List<RecommendationKnowledgeNote> knowledgeNotes
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        SearchRetrievalResult retrieval = lookupResult.getRetrieval();
        if (isChineseLocale(locale)) {
            return "候选来源："
                    + (lookupResult.isSqlFallbackUsed() ? "规则筛选回退" : "内容混合检索（关键词 + 语义）")
                    + "；关键词命中 " + retrieval.getLexicalHits()
                    + " 条，语义命中 " + retrieval.getVectorHits()
                    + " 条，最终选择 " + candidates.size()
                    + " 条视频，引用 " + knowledgeNotes.size() + " 条知识。";
        }
        return "Candidate source: "
                + (lookupResult.isSqlFallbackUsed() ? "rule-based fallback" : "hybrid content retrieval")
                + "; lexical hits " + retrieval.getLexicalHits()
                + ", semantic hits " + retrieval.getVectorHits()
                + ", selected " + candidates.size()
                + " videos, referenced " + knowledgeNotes.size() + " knowledge notes.";
    }

    private String buildEvidenceSummary(CandidateLookupResult lookupResult, List<RecommendationKnowledgeNote> knowledgeNotes) {
        Locale locale = LocaleContextHolder.getLocale();
        if (isChineseLocale(locale)) {
            return lookupResult.isSqlFallbackUsed()
                    ? "这次推荐采用安全规则和数据库筛选兜底，并用知识库补充训练说明。"
                    : "这次推荐综合了关键词检索、语义检索、安全规则和知识库引用。";
        }
        return lookupResult.isSqlFallbackUsed()
                ? "This recommendation used the rule-based database fallback and knowledge notes."
                : "This recommendation combines keyword retrieval, semantic retrieval, safety rules, and knowledge references.";
    }

    private List<WorkoutVideo> fallbackCandidates(ParsedRecommendationRequest parsed) {
        return workoutVideoService.findAllActive().stream()
                .filter(video -> parsed.getDurationMinutes() == null || video.getDurationMinutes() == null
                        || video.getDurationMinutes() <= parsed.getDurationMinutes() + 10)
                .filter(video -> !parsed.isKneeSensitive() || !"HIGH".equalsIgnoreCase(video.getImpactLevel()))
                .filter(video -> !parsed.isBackSensitive() || !"CORE".equalsIgnoreCase(video.getTargetBodyPart()))
                .sorted((left, right) -> Integer.compare(scoreVideo(parsed, right, null), scoreVideo(parsed, left, null)))
                .limit(3)
                .toList();
    }

    private List<WorkoutVideo> rankCandidates(ParsedRecommendationRequest parsed, List<WorkoutVideo> candidates, String latestFeedbackCode) {
        return candidates.stream()
                .sorted((left, right) -> Integer.compare(
                        scoreVideo(parsed, right, latestFeedbackCode),
                        scoreVideo(parsed, left, latestFeedbackCode)
                ))
                .toList();
    }

    private CandidateLookupResult findCandidates(
            ParsedRecommendationRequest parsed,
            String requestText,
            boolean relaxGoal,
            WorkoutVideoQuery query
    ) {
        SearchRetrievalResult retrieval = workoutVideoSearchService.searchCandidateIds(parsed, requestText, relaxGoal);
        if (!retrieval.getCandidateIds().isEmpty()) {
            List<WorkoutVideo> indexedCandidates = workoutVideoService.findByIdsPreservingOrder(retrieval.getCandidateIds()).stream()
                    .filter(video -> matchesSearchConstraints(video, parsed, relaxGoal))
                    .toList();
            if (!indexedCandidates.isEmpty()) {
                return CandidateLookupResult.builder()
                        .videos(indexedCandidates)
                        .retrieval(retrieval)
                        .sqlFallbackUsed(false)
                        .build();
            }
        }
        return CandidateLookupResult.builder()
                .videos(workoutVideoService.findCandidates(query))
                .retrieval(retrieval)
                .sqlFallbackUsed(true)
                .build();
    }

    private String buildRetrievalSummary(CandidateLookupResult lookupResult, boolean relaxedGoal) {
        Locale locale = LocaleContextHolder.getLocale();
        SearchRetrievalResult retrieval = lookupResult.getRetrieval();
        if (lookupResult.isSqlFallbackUsed()) {
            if (relaxedGoal) {
                return messageSource.getMessage("recommend.retrieval.sqlRelaxed", null, locale);
            }
            return messageSource.getMessage("recommend.retrieval.sqlFallback", null, locale);
        }

        String mode;
        if (retrieval.isLexicalUsed() && retrieval.isVectorUsed()) {
            mode = messageSource.getMessage("recommend.retrieval.mode.hybrid", null, locale);
        } else if (retrieval.isLexicalUsed()) {
            mode = messageSource.getMessage("recommend.retrieval.mode.lexical", null, locale);
        } else if (retrieval.isVectorUsed()) {
            mode = messageSource.getMessage("recommend.retrieval.mode.vector", null, locale);
        } else {
            mode = messageSource.getMessage("recommend.retrieval.mode.search", null, locale);
        }

        String summary = messageSource.getMessage(
                "recommend.retrieval.summary",
                new Object[]{mode, retrieval.getLexicalHits(), retrieval.getVectorHits()},
                locale
        );
        if (relaxedGoal) {
            return summary + " " + messageSource.getMessage("recommend.retrieval.goalRelaxed", null, locale);
        }
        return summary;
    }

    private List<RecommendationKnowledgeNote> resolveKnowledgeNotes(ParsedRecommendationRequest parsed, List<WorkoutVideo> candidates) {
        Locale locale = LocaleContextHolder.getLocale();
        List<RecommendationKnowledgeNote> importedNotes = knowledgeSearchService.search(parsed, candidates, locale)
                .stream()
                .map(this::toKnowledgeNote)
                .toList();
        List<RecommendationKnowledgeNote> staticNotes = recommendationKnowledgeService.selectNotes(parsed, candidates, locale);
        return Stream.concat(importedNotes.stream(), staticNotes.stream())
                .sorted((left, right) -> Integer.compare(right.getScore(), left.getScore()))
                .limit(3)
                .toList();
    }

    private RecommendationKnowledgeNote toKnowledgeNote(KnowledgeChunkHit hit) {
        RecommendationKnowledgeNote note = new RecommendationKnowledgeNote();
        note.setId("kb-" + hit.getChunkId());
        note.setTitle(hit.getTitle());
        note.setBody(clamp(normalizeDisplayText(hit.getContent()), 320));
        note.setScore((int) Math.max(1, Math.round(hit.getScore() * 10)));
        return note;
    }

    private String clamp(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3).trim() + "...";
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
        Locale locale = LocaleContextHolder.getLocale();
        if (isChineseLocale(locale)) {
            return parsed.getDurationMinutes() + " 分钟" + localizeLabel(parsed.getGoal(), locale) + "训练方案";
        }
        return parsed.getDurationMinutes() + "-minute " + humanizeGoal(parsed.getGoal()) + " session";
    }

    private String buildSummary(ParsedRecommendationRequest parsed, List<WorkoutVideo> candidates) {
        Locale locale = LocaleContextHolder.getLocale();
        String posture = parsed.getPostureType() == null ? "flexible posture" : humanizeEnum(parsed.getPostureType());
        String equipment = parsed.getEquipment() == null ? "available equipment" : humanizeEnum(parsed.getEquipment());
        String area = parsed.getTargetArea() == null ? "full-body support" : humanizeEnum(parsed.getTargetArea());
        String impact = parsed.getImpactLevel() == null ? "a flexible intensity level" : humanizeEnum(parsed.getImpactLevel()) + " impact";
        if (isChineseLocale(locale)) {
            String zhPosture = parsed.getPostureType() == null ? "姿势相对灵活" : localizeLabel(parsed.getPostureType(), locale);
            String zhEquipment = parsed.getEquipment() == null ? "器械条件可放宽" : localizeLabel(parsed.getEquipment(), locale);
            String zhArea = parsed.getTargetArea() == null ? "全身支持" : localizeLabel(parsed.getTargetArea(), locale);
            String zhImpact = parsed.getImpactLevel() == null ? "强度相对灵活" : localizeLabel(parsed.getImpactLevel(), locale) + "冲击";
            return "这次方案围绕 "
                    + parsed.getDurationMinutes()
                    + " 分钟、"
                    + zhPosture
                    + "、"
                    + zhEquipment
                    + "、"
                    + zhImpact
                    + "，并优先照顾"
                    + zhArea
                    + "需求。最终匹配到 "
                    + candidates.size()
                    + " 条更合适的精选视频。";
        }
        return "Built around " + parsed.getDurationMinutes() + " minutes, " + posture + ", " + equipment
                + ", " + impact + ", and a focus on " + area + ". " + candidates.size() + " curated videos matched best.";
    }

    private String buildExplanation(
            UserProfile profile,
            ParsedRecommendationRequest parsed,
            List<WorkoutVideo> candidates,
            List<RecommendationKnowledgeNote> knowledgeNotes
    ) {
        try {
            return llmGateway.generateExplanation(profile, parsed, candidates, knowledgeNotes);
        } catch (Exception ex) {
            Locale locale = LocaleContextHolder.getLocale();
            String fallback = "These recommendations were assembled from your profile, requested duration, posture, equipment access, and safety filters.";
            if (isChineseLocale(locale)) {
                fallback = "这组推荐基于你的画像、训练时长、姿势偏好、器械条件和安全过滤综合整理而成。";
            }
            String summary = recommendationKnowledgeService.summarizeNotes(knowledgeNotes);
            if (!summary.isBlank()) {
                if (isChineseLocale(locale)) {
                    return fallback + " 本次重点补充的训练建议包括：" + summary + "。";
                }
                return fallback + " Coach notes prioritized: " + summary + ".";
            }
            return fallback;
        }
    }

    private String buildSafetyNote(ParsedRecommendationRequest parsed, List<RecommendationKnowledgeNote> knowledgeNotes) {
        Locale locale = LocaleContextHolder.getLocale();
        String extraTip = knowledgeNotes.isEmpty() ? null : normalizeDisplayText(knowledgeNotes.get(0).getBody());
        if (parsed.getSafetyFlags().isEmpty()) {
            if (isChineseLocale(locale)) {
                if (extraTip == null || extraTip.isBlank()) {
                    return "当前没有额外安全警示。请保持舒适强度，并注意补水。";
                }
                return "当前没有额外安全警示。请保持舒适强度，并注意补水。 " + extraTip;
            }
            if (extraTip == null || extraTip.isBlank()) {
                return "No extra safety flags detected. Maintain comfortable intensity and hydrate well.";
            }
            return "No extra safety flags detected. Maintain comfortable intensity and hydrate well. " + extraTip;
        }
        if (isChineseLocale(locale)) {
            if (extraTip == null || extraTip.isBlank()) {
                return "已应用安全过滤：" + parsed.getSafetyFlags().stream()
                        .map(flag -> localizeSafetyFlag(flag, locale))
                        .collect(Collectors.joining("、")) + "。如果疼痛明显增加，请立即停止。";
            }
            return "已应用安全过滤：" + parsed.getSafetyFlags().stream()
                    .map(flag -> localizeSafetyFlag(flag, locale))
                    .collect(Collectors.joining("、")) + "。如果疼痛明显增加，请立即停止。 " + extraTip;
        }
        if (extraTip == null || extraTip.isBlank()) {
            return "Safety filters applied: " + String.join(", ", parsed.getSafetyFlags()) + ". Stop immediately if pain increases.";
        }
        return "Safety filters applied: " + String.join(", ", parsed.getSafetyFlags()) + ". Stop immediately if pain increases. " + extraTip;
    }

    private List<String> buildAppliedFilters(ParsedRecommendationRequest parsed) {
        Locale locale = LocaleContextHolder.getLocale();
        if (isChineseLocale(locale)) {
            return Stream.of(
                            parsed.getEquipment() == null ? null : "器械：" + localizeLabel(parsed.getEquipment(), locale),
                            parsed.getPostureType() == null ? null : "姿势：" + localizeLabel(parsed.getPostureType(), locale),
                            parsed.getTargetArea() == null ? null : "目标部位：" + localizeLabel(parsed.getTargetArea(), locale),
                            parsed.getImpactLevel() == null ? null : "冲击等级：" + localizeLabel(parsed.getImpactLevel(), locale),
                            parsed.getGoal() == null ? null : "目标：" + localizeLabel(parsed.getGoal(), locale),
                            parsed.isKneeSensitive() ? "膝盖敏感过滤" : null,
                            parsed.isBackSensitive() ? "背部友好过滤" : null,
                            parsed.getDurationMinutes() == null ? null : "时长 <= " + parsed.getDurationMinutes() + " 分钟"
                    )
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        }
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

    private List<String> buildFitReasons(
            ParsedRecommendationRequest parsed,
            List<WorkoutVideo> candidates,
            String latestFeedbackCode
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<String> reasons = new java.util.ArrayList<>();
        if (parsed.getDurationMinutes() != null) {
            reasons.add(isChineseLocale(locale)
                    ? "训练时长控制在 " + parsed.getDurationMinutes() + " 分钟以内，适合一次完整跟练。"
                    : "The session is kept within " + parsed.getDurationMinutes() + " minutes for a complete follow-along workout.");
        }
        if (parsed.getPostureType() != null) {
            reasons.add(isChineseLocale(locale)
                    ? "姿势偏好已匹配为：" + localizeLabel(parsed.getPostureType(), locale) + "。"
                    : "Posture preference matched: " + localizeLabel(parsed.getPostureType(), locale) + ".");
        }
        if (parsed.getEquipment() != null) {
            reasons.add(isChineseLocale(locale)
                    ? "器械条件已限制为：" + localizeLabel(parsed.getEquipment(), locale) + "。"
                    : "Equipment requirement matched: " + localizeLabel(parsed.getEquipment(), locale) + ".");
        }
        if (parsed.getTargetArea() != null) {
            reasons.add(isChineseLocale(locale)
                    ? "视频目标部位优先贴近：" + localizeLabel(parsed.getTargetArea(), locale) + "。"
                    : "Videos prioritize the requested body area: " + localizeLabel(parsed.getTargetArea(), locale) + ".");
        }
        if ("TOO_HARD".equals(latestFeedbackCode)) {
            reasons.add(isChineseLocale(locale)
                    ? "结合上次反馈，本次排序更偏向低冲击、初级和恢复类内容。"
                    : "Recent feedback shifted ranking toward lower-impact beginner or recovery content.");
        }
        if ("TOO_EASY".equals(latestFeedbackCode)) {
            reasons.add(isChineseLocale(locale)
                    ? "结合上次反馈，本次排序会适度提高训练挑战。"
                    : "Recent feedback shifted ranking toward a slightly higher challenge.");
        }
        if (reasons.isEmpty() && !candidates.isEmpty()) {
            reasons.add(isChineseLocale(locale)
                    ? "系统从当前内容库中选择了最接近需求且风险更低的视频组合。"
                    : "The system selected the closest lower-risk combination from the current library.");
        }
        return reasons;
    }

    private List<String> buildCautionItems(ParsedRecommendationRequest parsed, List<RecommendationKnowledgeNote> knowledgeNotes) {
        Locale locale = LocaleContextHolder.getLocale();
        List<String> cautions = new java.util.ArrayList<>();
        if (parsed.isKneeSensitive()) {
            cautions.add(isChineseLocale(locale)
                    ? "已排除高冲击内容；训练中避免跳跃、急停和快速深蹲。"
                    : "High-impact content was excluded; avoid jumping, abrupt stops, and fast deep squats.");
        }
        if (parsed.isBackSensitive()) {
            cautions.add(isChineseLocale(locale)
                    ? "已降低核心高负荷动作优先级；保持动作可控，不要追求幅度。"
                    : "High-load core work was de-prioritized; keep movements controlled instead of chasing range.");
        }
        if ("SITTING".equalsIgnoreCase(parsed.getPostureType())) {
            cautions.add(isChineseLocale(locale)
                    ? "坐姿训练也需要保持脚掌稳定、躯干直立，避免含胸和扭转过快。"
                    : "For seated workouts, keep feet stable, torso upright, and avoid fast twisting.");
        }
        if (cautions.isEmpty() && !knowledgeNotes.isEmpty()) {
            cautions.add(isChineseLocale(locale)
                    ? "本次没有明显禁忌，但仍建议按知识库提示保持舒适强度。"
                    : "No major contraindication was detected, but keep intensity comfortable according to the knowledge notes.");
        }
        return cautions;
    }

    private List<String> buildKnowledgeReferences(List<RecommendationKnowledgeNote> knowledgeNotes) {
        return knowledgeNotes.stream()
                .map(RecommendationKnowledgeNote::getTitle)
                .map(this::normalizeDisplayText)
                .distinct()
                .toList();
    }

    private List<String> buildFilteredOutReasons(
            ParsedRecommendationRequest parsed,
            boolean relaxedGoal,
            CandidateLookupResult lookupResult
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<String> reasons = new java.util.ArrayList<>();
        if (parsed.isKneeSensitive()) {
            reasons.add(isChineseLocale(locale)
                    ? "过滤规则：膝盖敏感时，不返回高冲击视频。"
                    : "Filter rule: high-impact videos are removed for knee-sensitive requests.");
        }
        if (parsed.isBackSensitive()) {
            reasons.add(isChineseLocale(locale)
                    ? "过滤规则：背部敏感时，降低高核心负荷视频的优先级。"
                    : "Filter rule: high core-load videos are de-prioritized for back-sensitive requests.");
        }
        if (parsed.getEquipment() != null) {
            reasons.add(isChineseLocale(locale)
                    ? "过滤规则：只保留符合器械条件或更低器械要求的视频。"
                    : "Filter rule: keep videos matching the equipment setup or requiring less equipment.");
        }
        if (relaxedGoal) {
            reasons.add(isChineseLocale(locale)
                    ? "当精确目标匹配不足时，系统会放宽训练目标，但不会放宽安全条件。"
                    : "When exact goal matches are limited, the goal can be relaxed but safety constraints remain.");
        }
        if (lookupResult.isSqlFallbackUsed()) {
            reasons.add(isChineseLocale(locale)
                    ? "检索无稳定候选时，系统切换到 SQL 规则路径保证仍有可执行结果。"
                    : "When retrieval candidates are weak, SQL rules provide an executable fallback.");
        }
        return reasons;
    }

    private String toEvidenceJson(RecommendationResult result) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "candidateSummary", result.getCandidateSummary(),
                    "evidenceSummary", result.getEvidenceSummary(),
                    "appliedFilters", result.getAppliedFilters(),
                    "fitReasons", result.getFitReasons(),
                    "cautionItems", result.getCautionItems(),
                    "knowledgeReferences", result.getKnowledgeReferences(),
                    "filteredOutReasons", result.getFilteredOutReasons(),
                    "retrievalSummary", result.getRetrievalSummary()
            ));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private Map<Long, List<String>> buildVideoReasons(
            ParsedRecommendationRequest parsed,
            List<WorkoutVideo> videos,
            String latestFeedbackCode
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        return videos.stream().collect(Collectors.toMap(
                WorkoutVideo::getId,
                video -> {
                    List<String> reasons = new java.util.ArrayList<>();
                    if (matches(video.getEquipmentRequirement(), parsed.getEquipment())) {
                        reasons.add(isChineseLocale(locale) ? "器械条件匹配" : "Equipment fits your setup");
                    }
                    if (matches(video.getPostureType(), parsed.getPostureType())) {
                        reasons.add(isChineseLocale(locale) ? "姿势符合你的请求" : "Posture matches your request");
                    }
                    if (matches(video.getTargetBodyPart(), parsed.getTargetArea()) || "FULL_BODY".equalsIgnoreCase(video.getTargetBodyPart())) {
                        reasons.add(isChineseLocale(locale) ? "目标部位基本对齐" : "Body area is aligned");
                    }
                    if (matches(video.getTargetGoal(), parsed.getGoal())) {
                        reasons.add(isChineseLocale(locale) ? "训练目标一致" : "Training goal is aligned");
                    }
                    if (matches(video.getImpactLevel(), parsed.getImpactLevel())) {
                        reasons.add(isChineseLocale(locale) ? "冲击等级符合你的需求" : "Impact level matches your request");
                    }
                    if (parsed.isKneeSensitive() && !"HIGH".equalsIgnoreCase(video.getImpactLevel())) {
                        reasons.add(isChineseLocale(locale) ? "更低冲击的选择" : "Lower-impact option");
                    }
                    if (parsed.isBackSensitive() && !"CORE".equalsIgnoreCase(video.getTargetBodyPart())) {
                        reasons.add(isChineseLocale(locale) ? "更背部友好的选择" : "Back-friendly choice");
                    }
                    if ("TOO_HARD".equals(latestFeedbackCode)) {
                        reasons.add(isChineseLocale(locale) ? "根据你上次反馈，优先选择了更温和的内容" : "Recent feedback pushed this toward a gentler option");
                    }
                    if ("TOO_EASY".equals(latestFeedbackCode)) {
                        reasons.add(isChineseLocale(locale) ? "根据你上次反馈，优先选择了更有挑战性的内容" : "Recent feedback pushed this toward a more challenging option");
                    }
                    if (reasons.isEmpty()) {
                        reasons.add(isChineseLocale(locale) ? "这是当前视频库里最接近且更安全的匹配" : "Closest safe match from the curated library");
                    }
                    return reasons;
                },
                (left, right) -> left,
                java.util.LinkedHashMap::new
        ));
    }

    private Map<Long, List<String>> buildScoreBreakdown(
            ParsedRecommendationRequest parsed,
            List<WorkoutVideo> videos,
            CandidateLookupResult lookupResult
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<Long, SearchCandidateScore> retrievalScores = lookupResult.getRetrieval().getCandidateScores();
        return videos.stream().collect(Collectors.toMap(
                WorkoutVideo::getId,
                video -> {
                    List<String> lines = new java.util.ArrayList<>();
                    SearchCandidateScore score = retrievalScores.get(video.getId());
                    if (score != null && !lookupResult.isSqlFallbackUsed()) {
                        lines.add(labelWithScore(
                                isChineseLocale(locale) ? "词法相关度" : "Lexical relevance",
                                score.getLexicalScore()
                        ));
                        lines.add(labelWithScore(
                                isChineseLocale(locale) ? "向量相关度" : "Vector relevance",
                                score.getVectorScore()
                        ));
                        lines.add(labelWithScore(
                                isChineseLocale(locale) ? "检索总分" : "Retrieval rank",
                                score.getFinalScore()
                        ));
                    }
                    lines.add(labelWithScore(
                            isChineseLocale(locale) ? "业务匹配度" : "Business fit",
                            scoreVideo(parsed, video, null) / 20.0d
                    ));
                    if (lookupResult.isSqlFallbackUsed()) {
                        lines.add(isChineseLocale(locale) ? "当前结果来自 SQL 回退路径" : "This result came from the SQL fallback path");
                    }
                    return lines;
                },
                (left, right) -> left,
                java.util.LinkedHashMap::new
        ));
    }

    private int scoreVideo(ParsedRecommendationRequest parsed, WorkoutVideo video, String latestFeedbackCode) {
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
        score += feedbackBoost(video, latestFeedbackCode, parsed);
        return score;
    }

    private int feedbackBoost(WorkoutVideo video, String latestFeedbackCode, ParsedRecommendationRequest parsed) {
        if (latestFeedbackCode == null) {
            return 0;
        }
        int score = 0;
        if ("TOO_HARD".equals(latestFeedbackCode)) {
            if ("LOW".equalsIgnoreCase(video.getImpactLevel())) {
                score += 4;
            }
            if ("BEGINNER".equalsIgnoreCase(video.getDifficulty())) {
                score += 3;
            }
            if ("RECOVERY".equalsIgnoreCase(video.getTargetGoal())) {
                score += 2;
            }
            if ("CHAIR".equalsIgnoreCase(video.getEquipmentRequirement()) || "SITTING".equalsIgnoreCase(video.getPostureType())) {
                score += 1;
            }
        }
        if ("TOO_EASY".equals(latestFeedbackCode)) {
            if (!parsed.isKneeSensitive() && !"LOW".equalsIgnoreCase(video.getImpactLevel())) {
                score += 3;
            }
            if ("INTERMEDIATE".equalsIgnoreCase(video.getDifficulty()) || "ADVANCED".equalsIgnoreCase(video.getDifficulty())) {
                score += 3;
            }
            if ("MUSCLE_TONE".equalsIgnoreCase(video.getTargetGoal()) || "CORE_STRENGTH".equalsIgnoreCase(video.getTargetGoal())) {
                score += 2;
            }
        }
        return score;
    }

    private String latestFeedbackCode(Long userId) {
        WorkoutLog latestLog = workoutLogMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkoutLog>()
                .eq(WorkoutLog::getUserId, userId)
                .eq(WorkoutLog::getStatus, "COMPLETED")
                .orderByDesc(WorkoutLog::getCompletedAt)
                .last("limit 1"));
        if (latestLog == null || latestLog.getFeedbackNote() == null || !latestLog.getFeedbackNote().startsWith("USER_FEEDBACK:")) {
            return null;
        }
        return latestLog.getFeedbackNote().substring("USER_FEEDBACK:".length());
    }

    private String applyRecentFeedbackAdjustment(ParsedRecommendationRequest parsed, String latestFeedbackCode) {
        Locale locale = LocaleContextHolder.getLocale();
        if (latestFeedbackCode == null) {
            return null;
        }
        if ("TOO_HARD".equals(latestFeedbackCode)) {
            if (parsed.getImpactLevel() == null || "HIGH".equalsIgnoreCase(parsed.getImpactLevel()) || "MEDIUM".equalsIgnoreCase(parsed.getImpactLevel())) {
                parsed.setImpactLevel("LOW");
            }
            if (parsed.getDurationMinutes() != null && parsed.getDurationMinutes() > 15) {
                parsed.setDurationMinutes(Math.max(10, parsed.getDurationMinutes() - 5));
            }
            return isChineseLocale(locale)
                    ? "根据你上次“太难”的反馈，这次会优先推荐更温和、时长稍短、冲击更低的内容。"
                    : "Based on your last “too hard” feedback, this round favors gentler, slightly shorter, lower-impact options.";
        }
        if ("TOO_EASY".equals(latestFeedbackCode)) {
            if (parsed.getImpactLevel() == null || "LOW".equalsIgnoreCase(parsed.getImpactLevel())) {
                parsed.setImpactLevel("MEDIUM");
            }
            if (parsed.getDurationMinutes() != null && parsed.getDurationMinutes() < 30) {
                parsed.setDurationMinutes(parsed.getDurationMinutes() + 5);
            }
            return isChineseLocale(locale)
                    ? "根据你上次“太轻松”的反馈，这次会优先推荐稍有挑战、时长略长的内容。"
                    : "Based on your last “too easy” feedback, this round favors slightly longer, more challenging options.";
        }
        if ("JUST_RIGHT".equals(latestFeedbackCode)) {
            return isChineseLocale(locale)
                    ? "系统检测到你上次的反馈是“刚好”，因此会延续当前训练强度方向。"
                    : "Your last feedback was “just right”, so the current training direction is being kept steady.";
        }
        return null;
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

    private boolean isChineseLocale(Locale locale) {
        return locale != null && locale.getLanguage().toLowerCase(Locale.ROOT).startsWith("zh");
    }

    private String localizeLabel(String value, Locale locale) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String key = "label." + value.toLowerCase(Locale.ROOT);
        return messageSource.getMessage(key, null, humanizeEnum(value), locale);
    }

    private String localizeSafetyFlag(String value, Locale locale) {
        return switch (value) {
            case "KNEE_SENSITIVE" -> isChineseLocale(locale) ? "膝盖敏感" : "Knee sensitive";
            case "BACK_SENSITIVE" -> isChineseLocale(locale) ? "背部敏感" : "Back sensitive";
            case "SITTING_REQUIRED" -> isChineseLocale(locale) ? "需要坐姿/椅子训练" : "Sitting required";
            default -> value;
        };
    }

    private String labelWithScore(String label, double score) {
        return label + ": " + Math.round(score * 100) + "%";
    }

    private String fallbackMessage(String type) {
        Locale locale = LocaleContextHolder.getLocale();
        if ("GOAL_RELAXED".equals(type)) {
            return isChineseLocale(locale)
                    ? "没有找到完全匹配的视频，因此系统放宽了训练目标，但保留了安全相关条件。"
                    : "No exact match was found, so the system relaxed the goal filter and kept safety-related conditions.";
        }
        return isChineseLocale(locale)
                ? "没有找到足够接近的内容，系统改用更稳妥的安全规则，从精选视频库中返回可执行方案。"
                : "No close database match was found, so the system returned the safest broadly relevant options from the curated library.";
    }

    private String normalizeDisplayText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\r", " ")
                .replace("\\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
