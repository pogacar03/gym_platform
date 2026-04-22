package com.graduation.fitmate.dto;

import lombok.Data;

@Data
public class WorkoutVideoQuery {
    private String goal;
    private Integer maxDurationMinutes;
    private String equipment;
    private String postureType;
    private String targetArea;
    private String impactLevel;
    private boolean kneeSensitive;
    private boolean backSensitive;
    private boolean relaxGoal;
}
