package com.graduation.fitmate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.graduation.fitmate.config.AppSearchProperties;
import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.SearchRetrievalResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkoutVideoSearchServiceTest {

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
        FakeWorkoutVideoSearchGateway searchGateway = new FakeWorkoutVideoSearchGateway();
        searchGateway.lexicalScores = Map.of(42L, 3.2d);
        searchGateway.vectorFailure = new IOException("vector search unavailable");
        WorkoutVideoSearchService service = new WorkoutVideoSearchService(
                searchGateway,
                properties,
                new EmbeddingService(properties)
        );

        ParsedRecommendationRequest parsed = new ParsedRecommendationRequest();
        parsed.setTargetArea("BACK");
        parsed.setPostureType("SITTING");

        SearchRetrievalResult result = service.searchCandidateIds(parsed, "seated back workout", false);

        assertEquals("workout_video_search", searchGateway.lexicalIndexName);
        assertEquals(8, searchGateway.lexicalCandidateLimit);
        assertEquals("seated back workout SITTING BACK", searchGateway.lexicalRequestText);
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
        FakeWorkoutVideoSearchGateway searchGateway = new FakeWorkoutVideoSearchGateway();
        searchGateway.lexicalScores = Map.of(10L, 4.0d, 20L, 2.0d);
        searchGateway.vectorScores = Map.of(20L, 0.9d, 30L, 0.8d);
        WorkoutVideoSearchService service = new WorkoutVideoSearchService(
                searchGateway,
                properties,
                new EmbeddingService(properties)
        );

        ParsedRecommendationRequest parsed = new ParsedRecommendationRequest();
        parsed.setTargetArea("BACK");

        SearchRetrievalResult result = service.searchCandidateIds(parsed, "back mobility", false);

        assertEquals("back mobility BACK", searchGateway.lexicalRequestText);
        assertEquals(List.of(20L, 10L, 30L), result.getCandidateIds());
        assertTrue(result.getCandidateScores().get(20L).getFinalScore() > result.getCandidateScores().get(10L).getFinalScore());
        assertTrue(result.getCandidateScores().get(30L).getVectorScore() > 0.0d);
    }

    private static class FakeWorkoutVideoSearchGateway implements WorkoutVideoSearchGateway {

        private Map<Long, Double> lexicalScores = Map.of();
        private Map<Long, Double> vectorScores = Map.of();
        private IOException lexicalFailure;
        private IOException vectorFailure;
        private String lexicalIndexName;
        private int lexicalCandidateLimit;
        private String lexicalRequestText;

        @Override
        public Map<Long, Double> lexicalSearch(
                String indexName,
                int candidateLimit,
                Query filterQuery,
                String requestText
        ) throws IOException {
            lexicalIndexName = indexName;
            lexicalCandidateLimit = candidateLimit;
            lexicalRequestText = requestText;
            if (lexicalFailure != null) {
                throw lexicalFailure;
            }
            return lexicalScores;
        }

        @Override
        public Map<Long, Double> vectorSearch(
                String indexName,
                int candidateLimit,
                int numCandidates,
                Query filterQuery,
                List<Float> queryVector
        ) throws IOException {
            if (vectorFailure != null) {
                throw vectorFailure;
            }
            return vectorScores;
        }
    }
}
