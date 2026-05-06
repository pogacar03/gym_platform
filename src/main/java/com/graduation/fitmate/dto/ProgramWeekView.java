package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.TrainingProgramWeek;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ProgramWeekView {
    private TrainingProgramWeek week;
    private final List<ProgramSessionView> sessions = new ArrayList<>();
}
