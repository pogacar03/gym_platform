package com.graduation.fitmate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.graduation.fitmate.config.AppSearchProperties;
import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.SearchRetrievalResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkoutVideoSearchServiceTest {

    @Mock
    private WorkoutVideoSearchGateway searchGateway;

    @Test
    void shouldKeepLexicalCandidatesWhenVectorQueryFails() throws Exception {
        AppSearchProperties properties = new AppSearchProperties(
                true,
                "workout_video_search",
                8,
                true,
                96,
                24,
                0.55d,
                0.45d,
                false,
                "knowledge_chunk_search",
                5
        );
        WorkoutVideoSearchService service = new WorkoutVideoSearchService(
                searchGateway,
                properties,
                new EmbeddingService(properties)
        );

        ParsedRecommendationRequest parsed = new ParsedRecommendationRequest();
        parsed.setTargetArea("BACK");
        parsed.setPostureType("SITTING");

        when(searchGateway.lexicalSearch(eq("workout_video_search"), eq(8), any(), eq("seated back workout")))
                .thenReturn(Map.of(42L, 3.2d));
        when(searchGateway.vectorSearch(eq("workout_video_search"), eq(8), eq(24), any(), any()))
                .thenThrow(new IOException("vector search unavailable"));

        SearchRetrievalResult result = service.searchCandidateIds(parsed, "seated back workout", false);

        assertEquals(List.of(42L), result.getCandidateIds());
        assertEquals(1, result.getLexicalHits());
        assertEquals(0, result.getVectorHits());
        assertTrue(result.isLexicalUsed());
        assertFalse(result.isVectorUsed());
        assertEquals(1.0d, result.getCandidateScores().get(42L).getLexicalScore());
        assertEquals(0.0d, result.getCandidateScores().get(42L).getVectorScore());
    }

    @Test
    void shouldMergeLexicalAndVectorScoresIntoFinalRanking() throws Exception {
        AppSearchProperties properties = new AppSearchProperties(
                true,
                "workout_video_search",
                8,
                true,
                96,
                24,
                0.55d,
                0.45d,
                false,
                "knowledge_chunk_search",
                5
        );
        WorkoutVideoSearchService service = new WorkoutVideoSearchService(
                searchGateway,
                properties,
                new EmbeddingService(properties)
        );

        ParsedRecommendationRequest parsed = new ParsedRecommendationRequest();
        parsed.setTargetArea("BACK");

        when(searchGateway.lexicalSearch(eq("workout_video_search"), eq(8), any(), eq("back mobility")))
                .thenReturn(Map.of(10L, 4.0d, 20L, 2.0d));
        when(searchGateway.vectorSearch(eq("workout_video_search"), eq(8), eq(24), any(), any()))
                .thenReturn(Map.of(20L, 0.9d, 30L, 0.8d));

        SearchRetrievalResult result = service.searchCandidateIds(parsed, "back mobility", false);

        assertEquals(List.of(20L, 10L, 30L), result.getCandidateIds());
        assertTrue(result.getCandidateScores().get(20L).getFinalScore() > result.getCandidateScores().get(10L).getFinalScore());
        assertTrue(result.getCandidateScores().get(30L).getVectorScore() > 0.0d);
    }
}
