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
    private String retrievalSummary;
    private String fallbackMessage;
    private String feedbackAdjustment;
    private String evidenceSummary;
    private String candidateSummary;
    private boolean fallbackUsed;
    private Long planId;
    private final List<String> appliedFilters = new ArrayList<>();
    private final List<String> fitReasons = new ArrayList<>();
    private final List<String> cautionItems = new ArrayList<>();
    private final List<String> knowledgeReferences = new ArrayList<>();
    private final List<String> filteredOutReasons = new ArrayList<>();
    private final List<RecommendationKnowledgeNote> knowledgeNotes = new ArrayList<>();
    private final Map<Long, List<String>> scoreBreakdown = new LinkedHashMap<>();
    private final Map<Long, List<String>> videoReasons = new LinkedHashMap<>();
    private final List<WorkoutVideo> videos = new ArrayList<>();
}
