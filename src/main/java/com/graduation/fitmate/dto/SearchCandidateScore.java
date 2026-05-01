package com.graduation.fitmate.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchCandidateScore {
    double lexicalScore;
    double vectorScore;
    double finalScore;
}
