package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.TrainingProgram;
import com.graduation.fitmate.entity.UserProgramEnrollment;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TrainingProgramView {
    private TrainingProgram program;
    private UserProgramEnrollment enrollment;
    private final List<ProgramWeekView> weeks = new ArrayList<>();
}
