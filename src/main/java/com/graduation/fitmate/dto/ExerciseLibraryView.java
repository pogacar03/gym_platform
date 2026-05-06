package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.Exercise;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ExerciseLibraryView {
    private int totalExercises;
    private int lowRiskCount;
    private int noEquipmentCount;
    private final List<Exercise> exercises = new ArrayList<>();
}
