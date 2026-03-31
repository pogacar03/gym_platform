package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RecommendationResult {
    private String title;
    private String explanation;
    private String safetyNotes;
    private boolean fallbackUsed;
    private Long planId;
    private final List<WorkoutVideo> videos = new ArrayList<>();
}

