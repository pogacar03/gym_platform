package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.WorkoutVideo;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CandidateLookupResult {
    List<WorkoutVideo> videos;
    SearchRetrievalResult retrieval;
    boolean sqlFallbackUsed;
}
