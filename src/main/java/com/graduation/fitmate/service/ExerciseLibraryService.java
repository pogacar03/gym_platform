package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.ExerciseLibraryView;
import com.graduation.fitmate.entity.Exercise;
import com.graduation.fitmate.mapper.ExerciseMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExerciseLibraryService {

    private final ExerciseMapper exerciseMapper;

    public ExerciseLibraryService(ExerciseMapper exerciseMapper) {
        this.exerciseMapper = exerciseMapper;
    }

    public ExerciseLibraryView buildLibrary() {
        List<Exercise> exercises = exerciseMapper.selectList(new LambdaQueryWrapper<Exercise>()
                .orderByAsc(Exercise::getPrimaryMuscle)
                .orderByAsc(Exercise::getName));
        ExerciseLibraryView view = new ExerciseLibraryView();
        view.getExercises().addAll(exercises.stream().limit(120).toList());
        view.setTotalExercises(exercises.size());
        view.setLowRiskCount((int) exercises.stream().filter(exercise -> !"HIGH".equalsIgnoreCase(exercise.getRiskLevel())).count());
        view.setNoEquipmentCount((int) exercises.stream().filter(exercise -> "NONE".equalsIgnoreCase(exercise.getEquipment())).count());
        return view;
    }
}
