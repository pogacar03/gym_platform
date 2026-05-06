package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.WorkoutPlan;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PlanSessionView {
    private WorkoutPlan plan;
    private List<PlanSessionItemView> items = new ArrayList<>();
    private Integer totalMinutes;
    private boolean completedToday;
    private LocalDateTime latestCompletedAt;
    private String latestFeedbackCode;
}
