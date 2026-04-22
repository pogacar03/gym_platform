package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.DashboardView;
import com.graduation.fitmate.dto.DashboardDayStatus;
import com.graduation.fitmate.entity.RecommendationHistory;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutLog;
import com.graduation.fitmate.entity.WorkoutPlan;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.RecommendationHistoryMapper;
import com.graduation.fitmate.mapper.WorkoutLogMapper;
import com.graduation.fitmate.mapper.WorkoutPlanMapper;
import java.time.format.TextStyle;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
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
    private final WorkoutLogMapper workoutLogMapper;
    private final RecommendationHistoryMapper recommendationHistoryMapper;
    private final MessageSource messageSource;

    public DashboardService(
            UserAccountService userAccountService,
            UserProfileService userProfileService,
            WorkoutVideoService workoutVideoService,
            WorkoutPlanMapper workoutPlanMapper,
            WorkoutLogMapper workoutLogMapper,
            RecommendationHistoryMapper recommendationHistoryMapper,
            MessageSource messageSource
    ) {
        this.userAccountService = userAccountService;
        this.userProfileService = userProfileService;
        this.workoutVideoService = workoutVideoService;
        this.workoutPlanMapper = workoutPlanMapper;
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
        view.getFeaturedVideos().addAll(workoutVideoService.findAllActive().stream().limit(4).toList());
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

    private String firstCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return csv.split(",")[0].trim();
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
