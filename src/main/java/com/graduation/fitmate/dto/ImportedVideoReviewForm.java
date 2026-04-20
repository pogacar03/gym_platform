package com.graduation.fitmate.dto;

import lombok.Data;

@Data
public class ImportedVideoReviewForm {
    private String suggestedGoal;
    private String suggestedEquipment;
    private String suggestedPosture;
    private String suggestedTargetArea;
    private String suggestedDifficulty;
    private String suggestedImpactLevel;
    private String suggestedExtraTags;
    private String reviewNote;
}
