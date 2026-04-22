package com.graduation.fitmate.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ParsedRecommendationRequest {
    private String goal;
    private Integer durationMinutes;
    private String equipment;
    private String postureType;
    private String targetArea;
    private String impactLevel;
    private boolean kneeSensitive;
    private boolean backSensitive;
    private final List<String> safetyFlags = new ArrayList<>();
}
