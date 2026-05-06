package com.graduation.fitmate.dto;

import lombok.Data;

@Data
public class PlanCompletionRequest {
    private String feedbackCode;
    private Integer actualMinutes;
    private Integer skippedItems;
    private String discomfortArea;
    private String note;
}
