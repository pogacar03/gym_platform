package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RecommendationResult {
    private String title;
    private String summary;
    private String explanation;
    private String safetyNotes;
    private String fallbackMessage;
    private boolean fallbackUsed;
    private Long planId;
    private final List<String> appliedFilters = new ArrayList<>();
    private final Map<Long, List<String>> videoReasons = new LinkedHashMap<>();
    private final List<WorkoutVideo> videos = new ArrayList<>();
}
