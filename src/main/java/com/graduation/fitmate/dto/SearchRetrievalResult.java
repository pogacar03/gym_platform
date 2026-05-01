package com.graduation.fitmate.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchRetrievalResult {
    List<Long> candidateIds;
    Map<Long, SearchCandidateScore> candidateScores;
    int lexicalHits;
    int vectorHits;
    boolean lexicalUsed;
    boolean vectorUsed;

    public static SearchRetrievalResult empty() {
        return SearchRetrievalResult.builder()
                .candidateIds(List.of())
                .candidateScores(Map.of())
                .lexicalHits(0)
                .vectorHits(0)
                .lexicalUsed(false)
                .vectorUsed(false)
                .build();
    }
}
