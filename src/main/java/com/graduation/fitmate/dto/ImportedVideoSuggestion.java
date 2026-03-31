package com.graduation.fitmate.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ImportedVideoSuggestion {
    private String goal;
    private String equipment;
    private String posture;
    private String targetArea;
    private String difficulty;
    private String impactLevel;
    private BigDecimal confidenceScore;
    private final List<String> safetyFlags = new ArrayList<>();
}

