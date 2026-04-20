package com.graduation.fitmate.dto;

import lombok.Data;

@Data
public class RecommendationRequest {
    private String requestText;
    private Integer quickDurationMinutes;
    private String quickPosture;
    private String quickEquipment;
    private String quickIntensity;
    private String quickTargetArea;
}
