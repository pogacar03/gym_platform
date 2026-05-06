package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.Exercise;
import com.graduation.fitmate.entity.TrainingProgramSessionItem;
import com.graduation.fitmate.entity.WorkoutVideo;
import lombok.Data;

@Data
public class ProgramItemView {
    private TrainingProgramSessionItem item;
    private Exercise exercise;
    private WorkoutVideo video;
}
