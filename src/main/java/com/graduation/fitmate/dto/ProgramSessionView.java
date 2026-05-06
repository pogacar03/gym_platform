package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.TrainingProgramSession;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ProgramSessionView {
    private TrainingProgramSession session;
    private final List<ProgramItemView> items = new ArrayList<>();
}
