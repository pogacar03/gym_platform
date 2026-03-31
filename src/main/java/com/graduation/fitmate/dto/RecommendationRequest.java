package com.graduation.fitmate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecommendationRequest {
    @NotBlank(message = "Please describe your workout need.")
    private String requestText;
}

