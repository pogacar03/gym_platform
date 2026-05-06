package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.WorkoutPlanItem;
import com.graduation.fitmate.entity.WorkoutVideo;
import lombok.Data;

@Data
public class PlanSessionItemView {
    private WorkoutPlanItem item;
    private WorkoutVideo video;
    private String focusLabel;
    private String trainingTarget;
}
