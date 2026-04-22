package com.graduation.fitmate.search;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutVideoSearchDocument {
    private Long videoId;
    private String title;
    private String description;
    private String goal;
    private String targetBodyPart;
    private String equipmentRequirement;
    private String postureType;
    private String impactLevel;
    private String difficulty;
    @Builder.Default
    private List<String> extraTags = new ArrayList<>();
    private String platformChannel;
    private Integer durationMinutes;
    private Boolean active;
    private Boolean curated;
    private String searchText;
}
