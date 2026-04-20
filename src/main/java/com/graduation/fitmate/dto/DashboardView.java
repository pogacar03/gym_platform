package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.WorkoutLog;
import com.graduation.fitmate.entity.WorkoutPlan;
import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DashboardView {
    private String displayName;
    private UserProfile profile;
    private WorkoutPlan latestPlan;
    private Integer dailyTargetMinutes;
    private Integer weeklyTargetMinutes;
    private Integer completedTodayMinutes;
    private Integer completedThisWeekMinutes;
    private int todayProgressPercent;
    private int weeklyProgressPercent;
    private int activeDaysThisWeek;
    private int completedPlanCount;
    private int recommendationCount;
    private int taggedVideoCount;
    private int tagCoveragePercent;
    private int missingTagCount;
    private String nextSuggestedSession;
    private String lastRecommendationSummary;
    private final List<String> focusTags = new ArrayList<>();
    private final List<DashboardDayStatus> weeklyRhythm = new ArrayList<>();
    private final List<WorkoutLog> recentLogs = new ArrayList<>();
    private final List<WorkoutVideo> featuredVideos = new ArrayList<>();
}
