package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.DashboardView;
import com.graduation.fitmate.dto.DashboardDayStatus;
import com.graduation.fitmate.dto.DashboardInsight;
import com.graduation.fitmate.entity.RecommendationHistory;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutLog;
import com.graduation.fitmate.entity.WorkoutPlan;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.RecommendationHistoryMapper;
import com.graduation.fitmate.mapper.WorkoutLogMapper;
import com.graduation.fitmate.mapper.WorkoutPlanItemMapper;
import com.graduation.fitmate.mapper.WorkoutPlanMapper;
import com.graduation.fitmate.util.BodyAreaMapper;
import com.graduation.fitmate.util.WorkoutFeedbackParser;
import java.time.format.TextStyle;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final UserAccountService userAccountService;
    private final UserProfileService userProfileService;
    private final WorkoutVideoService workoutVideoService;
    private final WorkoutPlanMapper workoutPlanMapper;
    private final WorkoutPlanItemMapper workoutPlanItemMapper;
    private final WorkoutLogMapper workoutLogMapper;
    private final RecommendationHistoryMapper recommendationHistoryMapper;
    private final MessageSource messageSource;

    public DashboardService(
            UserAccountService userAccountService,
            UserProfileService userProfileService,
            WorkoutVideoService workoutVideoService,
            WorkoutPlanMapper workoutPlanMapper,
            WorkoutPlanItemMapper workoutPlanItemMapper,
            WorkoutLogMapper workoutLogMapper,
            RecommendationHistoryMapper recommendationHistoryMapper,
            MessageSource messageSource
    ) {
        this.userAccountService = userAccountService;
        this.userProfileService = userProfileService;
        this.workoutVideoService = workoutVideoService;
        this.workoutPlanMapper = workoutPlanMapper;
        this.workoutPlanItemMapper = workoutPlanItemMapper;
        this.workoutLogMapper = workoutLogMapper;
        this.recommendationHistoryMapper = recommendationHistoryMapper;
        this.messageSource = messageSource;
    }

    public DashboardView buildDashboard(String username) {
        UserAccount account = userAccountService.findByUsername(username);
        UserProfile profile = userProfileService.getProfileByUsername(username);

        DashboardView view = new DashboardView();
        view.setDisplayName(account.getDisplayName() == null || account.getDisplayName().isBlank() ? account.getUsername() : account.getDisplayName());
        view.setProfile(profile);
        List<WorkoutVideo> activeVideos = workoutVideoService.findAllActive();
        view.getProfileRecommendedVideos().addAll(profileRecommendedVideos(profile, activeVideos));
        view.getFeaturedVideos().addAll(activeVideos.stream().limit(4).toList());
        view.setLatestPlan(findLatestPlan(account.getId()));
        view.setNextSuggestedSession(buildNextSuggestedSession(profile));
        view.setDailyTargetMinutes(profile != null && profile.getPreferredDurationMinutes() != null ? profile.getPreferredDurationMinutes() : 20);
        view.setWeeklyTargetMinutes(view.getDailyTargetMinutes() * (profile != null && profile.getWeeklyFrequency() != null ? profile.getWeeklyFrequency() : 3));

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay();
        List<WorkoutLog> completedLogs = workoutLogMapper.selectList(new LambdaQueryWrapper<WorkoutLog>()
                .eq(WorkoutLog::getUserId, account.getId())
                .eq(WorkoutLog::getStatus, "COMPLETED")
                .orderByDesc(WorkoutLog::getCompletedAt));

        int minutesPerCompletion = view.getDailyTargetMinutes();
        int completedToday = (int) completedLogs.stream()
                .filter(log -> log.getCompletedAt() != null && !log.getCompletedAt().isBefore(startOfDay))
                .count() * minutesPerCompletion;
        int completedWeek = (int) completedLogs.stream()
                .filter(log -> log.getCompletedAt() != null && !log.getCompletedAt().isBefore(startOfWeek))
                .count() * minutesPerCompletion;

        view.setCompletedTodayMinutes(completedToday);
        view.setCompletedThisWeekMinutes(completedWeek);
        view.setTodayProgressPercent(percent(completedToday, view.getDailyTargetMinutes()));
        view.setWeeklyProgressPercent(percent(completedWeek, view.getWeeklyTargetMinutes()));
        view.setActiveDaysThisWeek((int) completedLogs.stream()
                .filter(log -> log.getCompletedAt() != null && !log.getCompletedAt().isBefore(startOfWeek))
                .map(log -> log.getCompletedAt().toLocalDate())
                .distinct()
                .count());
        view.setCompletedPlanCount((int) completedLogs.size());
        Long recommendationCount = recommendationHistoryMapper.selectCount(new LambdaQueryWrapper<RecommendationHistory>()
                .eq(RecommendationHistory::getUserId, account.getId()));
        view.setRecommendationCount(recommendationCount == null ? 0 : recommendationCount.intValue());
        view.getRecentLogs().addAll(completedLogs.stream().limit(4).toList());
        WorkoutLog latestFeedbackLog = completedLogs.stream()
                .filter(log -> parseFeedbackCode(log.getFeedbackNote()) != null)
                .findFirst()
                .orElse(null);
        if (latestFeedbackLog != null) {
            String code = parseFeedbackCode(latestFeedbackLog.getFeedbackNote());
            view.setLatestFeedbackCode(code);
            view.setLatestFeedbackLabel(localizeFeedbackCode(code));
            view.setLatestFeedbackRecordedAt(latestFeedbackLog.getCompletedAt());
        }
        RecommendationHistory lastRecommendation = recommendationHistoryMapper.selectOne(new LambdaQueryWrapper<RecommendationHistory>()
                .eq(RecommendationHistory::getUserId, account.getId())
                .orderByDesc(RecommendationHistory::getCreatedAt)
                .last("limit 1"));
        if (lastRecommendation != null) {
            view.setLastRecommendationSummary(lastRecommendation.getRequestText());
        }

        int taggedCount = (int) view.getFeaturedVideos().stream().filter(this::isFullyTagged).count();
        view.setTaggedVideoCount(taggedCount);
        view.setMissingTagCount(Math.max(0, view.getFeaturedVideos().size() - taggedCount));
        view.setTagCoveragePercent(view.getFeaturedVideos().isEmpty() ? 0 : percent(taggedCount, view.getFeaturedVideos().size()));
        view.getFocusTags().addAll(Stream.of(
                        profile != null ? profile.getFitnessGoal() : null,
                        profile != null ? profile.getPosturePreference() : null,
                        profile != null ? firstCsv(profile.getTargetAreas()) : null,
                        profile != null ? firstCsv(profile.getAvailableEquipment()) : null
                )
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        view.getWeeklyRhythm().addAll(buildWeeklyRhythm(profile, completedLogs, startOfWeek.toLocalDate()));
        view.getSevenDayTrend().addAll(buildSevenDayTrend(completedLogs, today.minusDays(6), minutesPerCompletion));
        view.getFeedbackInsights().addAll(buildFeedbackInsights(completedLogs));
        view.getMuscleCoverageInsights().addAll(buildMuscleCoverageInsights(account.getId(), startOfWeek));
        view.setTrainingInsight(buildTrainingInsight(view));
        return view;
    }

    @Transactional
    public void completeLatestPlan(String username) {
        UserAccount account = userAccountService.findByUsername(username);
        WorkoutPlan latestPlan = findLatestPlan(account.getId());
        if (latestPlan == null) {
            return;
        }
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long alreadyCompletedToday = workoutLogMapper.selectCount(new LambdaQueryWrapper<WorkoutLog>()
                .eq(WorkoutLog::getUserId, account.getId())
                .eq(WorkoutLog::getPlanId, latestPlan.getId())
                .eq(WorkoutLog::getStatus, "COMPLETED")
                .ge(WorkoutLog::getCompletedAt, startOfDay));
        if (alreadyCompletedToday != null && alreadyCompletedToday > 0) {
            return;
        }
        WorkoutLog log = new WorkoutLog();
        log.setUserId(account.getId());
        log.setPlanId(latestPlan.getId());
        log.setStatus("COMPLETED");
        log.setFatigueLevel(3);
        log.setFeedbackNote("Completed from dashboard quick action");
        log.setCompletedAt(LocalDateTime.now());
        workoutLogMapper.insert(log);
    }

    @Transactional
    public void recordLatestPlanFeedback(String username, String feedbackCode) {
        UserAccount account = userAccountService.findByUsername(username);
        WorkoutPlan latestPlan = findLatestPlan(account.getId());
        if (latestPlan == null || feedbackCode == null || feedbackCode.isBlank()) {
            return;
        }
        WorkoutLog latestLog = workoutLogMapper.selectOne(new LambdaQueryWrapper<WorkoutLog>()
                .eq(WorkoutLog::getUserId, account.getId())
                .eq(WorkoutLog::getPlanId, latestPlan.getId())
                .eq(WorkoutLog::getStatus, "COMPLETED")
                .orderByDesc(WorkoutLog::getCompletedAt)
                .last("limit 1"));
        if (latestLog == null) {
            latestLog = new WorkoutLog();
            latestLog.setUserId(account.getId());
            latestLog.setPlanId(latestPlan.getId());
            latestLog.setStatus("COMPLETED");
            latestLog.setCompletedAt(LocalDateTime.now());
            latestLog.setFatigueLevel(mapFatigueLevel(feedbackCode));
            latestLog.setFeedbackNote(WorkoutFeedbackParser.build(feedbackCode, null, null, null, null));
            workoutLogMapper.insert(latestLog);
            return;
        }
        latestLog.setFatigueLevel(mapFatigueLevel(feedbackCode));
        latestLog.setFeedbackNote(WorkoutFeedbackParser.build(feedbackCode, null, null, null, null));
        if (latestLog.getCompletedAt() == null) {
            latestLog.setCompletedAt(LocalDateTime.now());
        }
        workoutLogMapper.updateById(latestLog);
    }

    private WorkoutPlan findLatestPlan(Long userId) {
        return workoutPlanMapper.selectOne(new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getUserId, userId)
                .orderByDesc(WorkoutPlan::getCreatedAt)
                .last("limit 1"));
    }

    private int percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, (int) Math.round(numerator * 100.0 / denominator)));
    }

    private boolean isFullyTagged(WorkoutVideo video) {
        return Stream.of(
                        video.getTargetGoal(),
                        video.getTargetBodyPart(),
                        video.getEquipmentRequirement(),
                        video.getImpactLevel(),
                        video.getPostureType()
                )
                .allMatch(value -> value != null && !value.isBlank());
    }

    private List<DashboardDayStatus> buildWeeklyRhythm(UserProfile profile, List<WorkoutLog> completedLogs, LocalDate weekStart) {
        int weeklyFrequency = profile != null && profile.getWeeklyFrequency() != null ? profile.getWeeklyFrequency() : 3;
        Set<LocalDate> completedDates = completedLogs.stream()
                .filter(log -> log.getCompletedAt() != null && !log.getCompletedAt().toLocalDate().isBefore(weekStart))
                .map(log -> log.getCompletedAt().toLocalDate())
                .collect(java.util.stream.Collectors.toSet());

        List<Integer> plannedIndexes = switch (weeklyFrequency) {
            case 1 -> List.of(2);
            case 2 -> List.of(1, 4);
            case 3 -> List.of(0, 2, 4);
            case 4 -> List.of(0, 2, 4, 6);
            case 5 -> List.of(0, 1, 3, 4, 6);
            default -> List.of(0, 2, 4);
        };

        List<DashboardDayStatus> days = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            days.add(new DashboardDayStatus(
                    day.getDayOfWeek().getDisplayName(TextStyle.SHORT, LocaleContextHolder.getLocale()),
                    plannedIndexes.contains(i),
                    completedDates.contains(day)
            ));
        }
        return days;
    }

    private List<WorkoutVideo> profileRecommendedVideos(UserProfile profile, List<WorkoutVideo> activeVideos) {
        return activeVideos.stream()
                .filter(video -> video.getDurationMinutes() != null)
                .filter(this::isFollowAlongContent)
                .filter(video -> isSafeForProfile(video, profile))
                .filter(video -> !hasShoulderConcern(profile) || isShoulderFriendlyContent(video))
                .sorted((left, right) -> Integer.compare(scoreForProfile(profile, right), scoreForProfile(profile, left)))
                .limit(4)
                .toList();
    }

    private boolean isSafeForProfile(WorkoutVideo video, UserProfile profile) {
        if (profile == null) {
            return true;
        }
        if (Boolean.TRUE.equals(profile.getKneeSensitive()) && "HIGH".equalsIgnoreCase(video.getImpactLevel())) {
            return false;
        }
        if (Boolean.TRUE.equals(profile.getBackSensitive()) && "CORE".equalsIgnoreCase(video.getTargetBodyPart())) {
            return false;
        }
        if (hasShoulderConcern(profile) && "HIGH".equalsIgnoreCase(video.getImpactLevel())) {
            return false;
        }
        return true;
    }

    private boolean isFollowAlongContent(WorkoutVideo video) {
        String text = Stream.of(video.getTitle(), video.getDescription(), video.getExtraTags())
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
        boolean hasWorkoutSignal = Stream.of(
                        "workout",
                        "exercise",
                        "routine",
                        "follow along",
                        "stretch",
                        "mobility",
                        "training",
                        "walk",
                        "pilates",
                        "cardio",
                        "strength",
                        "分钟",
                        "训练",
                        "拉伸"
                )
                .anyMatch(text::contains);
        boolean hasNonWorkoutSignal = Stream.of(
                        "this week in fitness",
                        "news",
                        "report",
                        "review",
                        "explained",
                        "erklärt",
                        "podcast"
                )
                .anyMatch(text::contains);
        return hasWorkoutSignal && !hasNonWorkoutSignal;
    }

    private int scoreForProfile(UserProfile profile, WorkoutVideo video) {
        int score = 0;
        if (profile == null) {
            return score + durationScore(20, video);
        }
        if (matches(video.getTargetGoal(), profile.getFitnessGoal())) {
            score += 5;
        }
        if (matches(video.getPostureType(), profile.getPosturePreference())) {
            score += 4;
        }
        if (csvContainsBodyArea(profile.getTargetAreas(), video.getTargetBodyPart()) || "FULL_BODY".equalsIgnoreCase(video.getTargetBodyPart())) {
            score += 4;
        }
        if (equipmentCompatible(profile.getAvailableEquipment(), video.getEquipmentRequirement())) {
            score += 4;
        }
        if (Boolean.TRUE.equals(profile.getKneeSensitive()) && !"HIGH".equalsIgnoreCase(video.getImpactLevel())) {
            score += 3;
        }
        if (Boolean.TRUE.equals(profile.getBackSensitive()) && !"CORE".equalsIgnoreCase(video.getTargetBodyPart())) {
            score += 2;
        }
        if (hasShoulderConcern(profile)) {
            if (isShoulderFriendlyContent(video)) {
                score += 8;
            }
            if ("ARMS".equalsIgnoreCase(video.getTargetBodyPart()) || "FULL_BODY".equalsIgnoreCase(video.getTargetBodyPart())) {
                score += 4;
            }
            if ("RECOVERY".equalsIgnoreCase(video.getTargetGoal())) {
                score += 3;
            }
            if ("NONE".equalsIgnoreCase(video.getEquipmentRequirement())) {
                score += 2;
            }
            if ("DUMBBELL".equalsIgnoreCase(video.getEquipmentRequirement())) {
                score -= 3;
            }
        }
        score += durationScore(profile.getPreferredDurationMinutes(), video);
        return score;
    }

    private boolean isShoulderFriendlyContent(WorkoutVideo video) {
        String text = Stream.of(video.getTitle(), video.getDescription(), video.getExtraTags())
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
        return Stream.of(
                        "shoulder",
                        "upper body",
                        "arm workout",
                        "arms",
                        "mobility",
                        "stretch",
                        "rotator cuff",
                        "frozen shoulder",
                        "肩"
                )
                .anyMatch(text::contains);
    }

    private int durationScore(Integer preferredDuration, WorkoutVideo video) {
        if (preferredDuration == null || video.getDurationMinutes() == null) {
            return 0;
        }
        int distance = Math.abs(video.getDurationMinutes() - preferredDuration);
        if (distance <= 5) {
            return 4;
        }
        if (distance <= 10) {
            return 2;
        }
        return 0;
    }

    private boolean equipmentCompatible(String availableEquipment, String requiredEquipment) {
        if (requiredEquipment == null || requiredEquipment.isBlank() || "NONE".equalsIgnoreCase(requiredEquipment)) {
            return true;
        }
        return csvContains(availableEquipment, requiredEquipment);
    }

    private boolean csvContains(String csv, String value) {
        if (csv == null || csv.isBlank() || value == null || value.isBlank()) {
            return false;
        }
        return Stream.of(csv.split(","))
                .map(String::trim)
                .anyMatch(item -> item.equalsIgnoreCase(value));
    }

    private boolean csvContainsBodyArea(String csv, String value) {
        if (csv == null || csv.isBlank() || value == null || value.isBlank()) {
            return false;
        }
        return Stream.of(csv.split(","))
                .map(String::trim)
                .anyMatch(item -> BodyAreaMapper.sameArea(item, value));
    }

    private boolean matches(String actual, String expected) {
        return actual != null && expected != null && actual.equalsIgnoreCase(expected);
    }

    private boolean hasShoulderConcern(UserProfile profile) {
        String notes = profile == null || profile.getInjuryNotes() == null ? "" : profile.getInjuryNotes().toLowerCase(Locale.ROOT);
        return Stream.of("肩", "肩周炎", "shoulder", "rotator cuff", "frozen shoulder")
                .anyMatch(notes::contains);
    }

    private String firstCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return csv.split(",")[0].trim();
    }

    private Integer mapFatigueLevel(String feedbackCode) {
        return switch (feedbackCode) {
            case "TOO_EASY" -> 1;
            case "JUST_RIGHT" -> 3;
            case "TOO_HARD" -> 5;
            default -> 3;
        };
    }

    private String parseFeedbackCode(String note) {
        return WorkoutFeedbackParser.code(note);
    }

    private List<DashboardInsight> buildSevenDayTrend(List<WorkoutLog> completedLogs, LocalDate startDate, int minutesPerCompletion) {
        Set<LocalDate> completedDates = completedLogs.stream()
                .filter(log -> log.getCompletedAt() != null && !log.getCompletedAt().toLocalDate().isBefore(startDate))
                .map(log -> log.getCompletedAt().toLocalDate())
                .collect(Collectors.toSet());
        List<DashboardInsight> insights = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            DashboardInsight insight = new DashboardInsight();
            insight.setLabel(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, LocaleContextHolder.getLocale()));
            insight.setValue(completedDates.contains(day) ? minutesPerCompletion + " min" : "0 min");
            insight.setPercent(completedDates.contains(day) ? 100 : 0);
            insights.add(insight);
        }
        return insights;
    }

    private List<DashboardInsight> buildFeedbackInsights(List<WorkoutLog> completedLogs) {
        List<String> feedbackCodes = completedLogs.stream()
                .map(log -> WorkoutFeedbackParser.code(log.getFeedbackNote()))
                .filter(Objects::nonNull)
                .limit(12)
                .toList();
        int total = feedbackCodes.size();
        if (total == 0) {
            return List.of();
        }
        return Stream.of("TOO_EASY", "JUST_RIGHT", "TOO_HARD")
                .map(code -> {
                    int count = (int) feedbackCodes.stream().filter(code::equals).count();
                    DashboardInsight insight = new DashboardInsight();
                    insight.setLabel(localizeFeedbackCode(code));
                    insight.setValue(String.valueOf(count));
                    insight.setPercent(percent(count, total));
                    return insight;
                })
                .toList();
    }

    private List<DashboardInsight> buildMuscleCoverageInsights(Long userId, LocalDateTime startOfWeek) {
        List<WorkoutPlan> recentPlans = workoutPlanMapper.selectList(new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getUserId, userId)
                .ge(WorkoutPlan::getCreatedAt, startOfWeek)
                .orderByDesc(WorkoutPlan::getCreatedAt)
                .last("limit 10"));
        if (recentPlans.isEmpty()) {
            return List.of();
        }
        List<Long> planIds = recentPlans.stream().map(WorkoutPlan::getId).toList();
        return buildMuscleCoverageFromPlans(planIds);
    }

    private List<DashboardInsight> buildMuscleCoverageFromPlans(List<Long> planIds) {
        Map<String, Long> counts = planIds.stream()
                .flatMap(planId -> workoutVideoService.findByIdsPreservingOrder(workoutPlanItemMapperIds(planId)).stream())
                .map(WorkoutVideo::getTargetBodyPart)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return List.of();
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(4)
                .map(entry -> {
                    DashboardInsight insight = new DashboardInsight();
                    insight.setLabel(messageSource.getMessage("label." + entry.getKey().toLowerCase(Locale.ROOT), null, entry.getKey(), LocaleContextHolder.getLocale()));
                    insight.setValue(String.valueOf(entry.getValue()));
                    insight.setPercent(percent(entry.getValue().intValue(), (int) total));
                    return insight;
                })
                .toList();
    }

    private List<Long> workoutPlanItemMapperIds(Long planId) {
        return workoutPlanItemMapper.selectList(new LambdaQueryWrapper<com.graduation.fitmate.entity.WorkoutPlanItem>()
                        .eq(com.graduation.fitmate.entity.WorkoutPlanItem::getPlanId, planId)
                        .orderByAsc(com.graduation.fitmate.entity.WorkoutPlanItem::getSortOrder))
                .stream()
                .map(com.graduation.fitmate.entity.WorkoutPlanItem::getVideoId)
                .toList();
    }

    private String buildTrainingInsight(DashboardView view) {
        Locale locale = LocaleContextHolder.getLocale();
        if (view.getCompletedThisWeekMinutes() >= view.getWeeklyTargetMinutes()) {
            return messageSource.getMessage("dashboard.insight.onTrack", null, "You are on track this week. Keep the next session steady rather than chasing extra intensity.", locale);
        }
        if ("TOO_HARD".equals(view.getLatestFeedbackCode())) {
            return messageSource.getMessage("dashboard.insight.tooHard", null, "Your latest feedback says the plan felt hard, so the next recommendation should stay shorter and lower impact.", locale);
        }
        if ("TOO_EASY".equals(view.getLatestFeedbackCode())) {
            return messageSource.getMessage("dashboard.insight.tooEasy", null, "Your latest feedback says the plan felt easy, so FitMate can raise the challenge slightly.", locale);
        }
        return messageSource.getMessage("dashboard.insight.default", null, "Complete one guided session and add feedback to unlock better personalization.", locale);
    }

    private String localizeFeedbackCode(String code) {
        Locale locale = LocaleContextHolder.getLocale();
        return switch (code) {
            case "TOO_EASY" -> messageSource.getMessage("dashboard.feedback.easy", null, "Too easy", locale);
            case "JUST_RIGHT" -> messageSource.getMessage("dashboard.feedback.justRight", null, "Just right", locale);
            case "TOO_HARD" -> messageSource.getMessage("dashboard.feedback.hard", null, "Too hard", locale);
            default -> code;
        };
    }

    private String buildNextSuggestedSession(UserProfile profile) {
        Locale locale = LocaleContextHolder.getLocale();
        int duration = profile != null && profile.getPreferredDurationMinutes() != null ? profile.getPreferredDurationMinutes() : 20;
        String goal = profile != null && profile.getFitnessGoal() != null
                ? messageSource.getMessage("label." + profile.getFitnessGoal().toLowerCase(Locale.ROOT), null, profile.getFitnessGoal(), locale)
                : messageSource.getMessage("dashboard.generalGoal", null, "general fitness", locale);
        return messageSource.getMessage(
                "dashboard.nextTemplate",
                new Object[]{duration, goal},
                duration + "-minute " + goal + " session",
                locale
        );
    }
}
