package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.PlanCompletionRequest;
import com.graduation.fitmate.dto.PlanSessionItemView;
import com.graduation.fitmate.dto.PlanSessionView;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.entity.WorkoutLog;
import com.graduation.fitmate.entity.WorkoutPlan;
import com.graduation.fitmate.entity.WorkoutPlanItem;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.WorkoutLogMapper;
import com.graduation.fitmate.mapper.WorkoutPlanItemMapper;
import com.graduation.fitmate.mapper.WorkoutPlanMapper;
import com.graduation.fitmate.util.UiDisplayHelper;
import com.graduation.fitmate.util.WorkoutFeedbackParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {

    private final UserAccountService userAccountService;
    private final WorkoutVideoService workoutVideoService;
    private final WorkoutPlanMapper workoutPlanMapper;
    private final WorkoutPlanItemMapper workoutPlanItemMapper;
    private final WorkoutLogMapper workoutLogMapper;
    private final UiDisplayHelper uiDisplayHelper;

    public PlanService(
            UserAccountService userAccountService,
            WorkoutVideoService workoutVideoService,
            WorkoutPlanMapper workoutPlanMapper,
            WorkoutPlanItemMapper workoutPlanItemMapper,
            WorkoutLogMapper workoutLogMapper,
            UiDisplayHelper uiDisplayHelper
    ) {
        this.userAccountService = userAccountService;
        this.workoutVideoService = workoutVideoService;
        this.workoutPlanMapper = workoutPlanMapper;
        this.workoutPlanItemMapper = workoutPlanItemMapper;
        this.workoutLogMapper = workoutLogMapper;
        this.uiDisplayHelper = uiDisplayHelper;
    }

    public PlanSessionView getPlanSession(String username, Long planId) {
        UserAccount account = userAccountService.findByUsername(username);
        WorkoutPlan plan = workoutPlanMapper.selectOne(new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getId, planId)
                .eq(WorkoutPlan::getUserId, account.getId())
                .last("limit 1"));
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found.");
        }

        List<WorkoutPlanItem> planItems = workoutPlanItemMapper.selectList(new LambdaQueryWrapper<WorkoutPlanItem>()
                .eq(WorkoutPlanItem::getPlanId, plan.getId())
                .orderByAsc(WorkoutPlanItem::getSortOrder));
        PlanSessionView view = new PlanSessionView();
        view.setPlan(plan);

        int totalMinutes = 0;
        for (WorkoutPlanItem item : planItems) {
            WorkoutVideo video = workoutVideoService.findById(item.getVideoId());
            if (video == null) {
                continue;
            }
            if (video.getDurationMinutes() != null) {
                totalMinutes += video.getDurationMinutes();
            }
            PlanSessionItemView itemView = new PlanSessionItemView();
            itemView.setItem(item);
            itemView.setVideo(video);
            itemView.setFocusLabel(uiDisplayHelper.label(video.getTargetBodyPart()));
            itemView.setTrainingTarget(buildTrainingTarget(video));
            view.getItems().add(itemView);
        }
        view.setTotalMinutes(totalMinutes);

        WorkoutLog latestLog = workoutLogMapper.selectOne(new LambdaQueryWrapper<WorkoutLog>()
                .eq(WorkoutLog::getUserId, account.getId())
                .eq(WorkoutLog::getPlanId, plan.getId())
                .eq(WorkoutLog::getStatus, "COMPLETED")
                .orderByDesc(WorkoutLog::getCompletedAt)
                .last("limit 1"));
        if (latestLog != null) {
            view.setLatestCompletedAt(latestLog.getCompletedAt());
            view.setLatestFeedbackCode(parseFeedbackCode(latestLog.getFeedbackNote()));
            view.setCompletedToday(latestLog.getCompletedAt() != null
                    && !latestLog.getCompletedAt().isBefore(LocalDate.now().atStartOfDay()));
        }
        return view;
    }

    @Transactional
    public void completePlan(String username, Long planId, PlanCompletionRequest request) {
        UserAccount account = userAccountService.findByUsername(username);
        WorkoutPlan plan = workoutPlanMapper.selectOne(new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getId, planId)
                .eq(WorkoutPlan::getUserId, account.getId())
                .last("limit 1"));
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found.");
        }
        WorkoutLog log = new WorkoutLog();
        log.setUserId(account.getId());
        log.setPlanId(plan.getId());
        log.setStatus("COMPLETED");
        String feedbackCode = request == null ? null : request.getFeedbackCode();
        log.setFatigueLevel(mapFatigueLevel(feedbackCode));
        log.setFeedbackNote(request == null
                ? WorkoutFeedbackParser.build(feedbackCode, null, null, null, null)
                : WorkoutFeedbackParser.build(
                feedbackCode,
                request.getActualMinutes(),
                request.getSkippedItems(),
                request.getDiscomfortArea(),
                request.getNote()));
        log.setCompletedAt(LocalDateTime.now());
        workoutLogMapper.insert(log);
    }

    private String buildTrainingTarget(WorkoutVideo video) {
        String goal = uiDisplayHelper.label(video.getTargetGoal());
        String posture = uiDisplayHelper.label(video.getPostureType());
        String equipment = uiDisplayHelper.label(video.getEquipmentRequirement());
        return goal + " · " + posture + " · " + equipment;
    }

    private Integer mapFatigueLevel(String feedbackCode) {
        if ("TOO_EASY".equalsIgnoreCase(feedbackCode)) {
            return 1;
        }
        if ("TOO_HARD".equalsIgnoreCase(feedbackCode)) {
            return 5;
        }
        return 3;
    }

    private String parseFeedbackCode(String feedbackNote) {
        return WorkoutFeedbackParser.code(feedbackNote);
    }
}
