package com.graduation.fitmate.service;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.graduation.fitmate.config.AppSearchProperties;
import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.dto.SearchCandidateScore;
import com.graduation.fitmate.dto.SearchRetrievalResult;
import com.graduation.fitmate.util.BodyAreaMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkoutVideoSearchService {

    private final WorkoutVideoSearchGateway searchGateway;
    private final AppSearchProperties searchProperties;
    private final EmbeddingService embeddingService;

    public WorkoutVideoSearchService(
            WorkoutVideoSearchGateway searchGateway,
            AppSearchProperties searchProperties,
            EmbeddingService embeddingService
    ) {
        this.searchGateway = searchGateway;
        this.searchProperties = searchProperties;
        this.embeddingService = embeddingService;
    }

    public SearchRetrievalResult searchCandidateIds(ParsedRecommendationRequest parsed, String requestText, boolean relaxGoal) {
        if (!searchProperties.enabled()) {
            return SearchRetrievalResult.empty();
        }

        Query filterQuery = Query.of(query -> query.bool(buildFilterQuery(parsed, relaxGoal).build()));
        String expandedSearchText = buildVectorText(parsed, requestText);
        Map<Long, Double> lexicalScores = lexicalSearchSafely(filterQuery, expandedSearchText);
        Map<Long, Double> vectorScores = vectorSearchSafely(parsed, requestText, filterQuery);

        if (lexicalScores.isEmpty() && vectorScores.isEmpty()) {
            return SearchRetrievalResult.empty();
        }
        List<Long> merged = mergeScores(lexicalScores, vectorScores);
        if (log.isDebugEnabled()) {
            log.debug(
                    "Hybrid retrieval completed: lexicalHits={}, vectorHits={}, mergedIds={}",
                    lexicalScores.size(),
                    vectorScores.size(),
                    merged
            );
        }
        return SearchRetrievalResult.builder()
                .candidateIds(merged)
                .candidateScores(buildScoreBreakdown(merged, lexicalScores, vectorScores))
                .lexicalHits(lexicalScores.size())
                .vectorHits(vectorScores.size())
                .lexicalUsed(!lexicalScores.isEmpty())
                .vectorUsed(!vectorScores.isEmpty())
                .build();
    }

    private Map<Long, Double> lexicalSearch(Query filterQuery, String requestText) throws IOException {
        return searchGateway.lexicalSearch(
                searchProperties.indexName(),
                searchProperties.candidateLimit(),
                filterQuery,
                requestText
        );
    }

    private Map<Long, Double> lexicalSearchSafely(Query filterQuery, String requestText) {
        try {
            return lexicalSearch(filterQuery, requestText);
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Elasticsearch lexical search failed: {}", ex.getMessage());
            return Map.of();
        }
    }

    private Map<Long, Double> vectorSearch(
            ParsedRecommendationRequest parsed,
            String requestText,
            Query filterQuery
    ) throws IOException {
        if (!searchProperties.vectorEnabled()) {
            return Map.of();
        }
        String vectorText = buildVectorText(parsed, requestText);
        if (!hasText(vectorText)) {
            return Map.of();
        }

        return searchGateway.vectorSearch(
                searchProperties.indexName(),
                searchProperties.candidateLimit(),
                searchProperties.vectorCandidates(),
                filterQuery,
                embeddingService.embed(vectorText)
        );
    }

    private Map<Long, Double> vectorSearchSafely(
            ParsedRecommendationRequest parsed,
            String requestText,
            Query filterQuery
    ) {
        try {
            return vectorSearch(parsed, requestText, filterQuery);
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Elasticsearch vector search failed; keeping lexical results: {}", ex.getMessage());
            return Map.of();
        }
    }

    private List<Long> mergeScores(Map<Long, Double> lexicalScores, Map<Long, Double> vectorScores) {
        double lexicalMax = lexicalScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0d);
        double vectorMax = vectorScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0d);

        Map<Long, Double> merged = new LinkedHashMap<>();
        lexicalScores.forEach((id, score) -> merged.put(
                id,
                normalized(score, lexicalMax) * searchProperties.lexicalWeight()
        ));
        vectorScores.forEach((id, score) -> merged.merge(
                id,
                normalized(score, vectorMax) * searchProperties.vectorWeight(),
                Double::sum
        ));

        return merged.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(Math.max(3, searchProperties.candidateLimit()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<Long, SearchCandidateScore> buildScoreBreakdown(
            List<Long> mergedIds,
            Map<Long, Double> lexicalScores,
            Map<Long, Double> vectorScores
    ) {
        double lexicalMax = lexicalScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0d);
        double vectorMax = vectorScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0d);
        Map<Long, SearchCandidateScore> scoreMap = new LinkedHashMap<>();
        for (Long id : mergedIds) {
            double lexical = normalized(lexicalScores.getOrDefault(id, 0.0d), lexicalMax);
            double vector = normalized(vectorScores.getOrDefault(id, 0.0d), vectorMax);
            double finalScore = lexical * searchProperties.lexicalWeight() + vector * searchProperties.vectorWeight();
            scoreMap.put(id, SearchCandidateScore.builder()
                    .lexicalScore(lexical)
                    .vectorScore(vector)
                    .finalScore(finalScore)
                    .build());
        }
        return scoreMap;
    }

    private double normalized(double score, double maxScore) {
        if (maxScore <= 0.0d) {
            return 0.0d;
        }
        return score / maxScore;
    }

    private BoolQuery.Builder buildFilterQuery(ParsedRecommendationRequest parsed, boolean relaxGoal) {
        BoolQuery.Builder bool = new BoolQuery.Builder()
                .filter(termQuery("active", true))
                .filter(termQuery("curated", true));

        if (!relaxGoal && parsed.isExplicitGoal() && hasText(parsed.getGoal())) {
            bool.filter(termQuery("goal", parsed.getGoal()));
        }
        if (parsed.isExplicitPosture() && hasText(parsed.getPostureType())) {
            bool.filter(termQuery("postureType", parsed.getPostureType()));
        }
        if (parsed.isExplicitTargetArea() && hasText(parsed.getTargetArea())) {
            bool.filter(termsQuery("targetBodyPart", List.of(BodyAreaMapper.toCanonical(parsed.getTargetArea()), "FULL_BODY")));
        }
        if (parsed.isExplicitImpactLevel() && hasText(parsed.getImpactLevel())) {
            bool.filter(termQuery("impactLevel", parsed.getImpactLevel()));
        }

        applyEquipmentRules(bool, parsed);

        if (parsed.isKneeSensitive()) {
            bool.mustNot(termQuery("impactLevel", "HIGH"));
        }
        if (parsed.isBackSensitive()) {
            bool.mustNot(termQuery("targetBodyPart", "CORE"));
        }
        if (parsed.isShoulderSensitive()) {
            bool.mustNot(termQuery("impactLevel", "HIGH"));
        }
        return bool;
    }

    private void applyEquipmentRules(BoolQuery.Builder bool, ParsedRecommendationRequest parsed) {
        if (!parsed.isExplicitEquipment() || !hasText(parsed.getEquipment())) {
            return;
        }
        if ("NONE".equalsIgnoreCase(parsed.getEquipment()) && "SITTING".equalsIgnoreCase(parsed.getPostureType())) {
            bool.filter(termsQuery("equipmentRequirement", List.of("NONE", "CHAIR")));
            return;
        }
        if ("NONE".equalsIgnoreCase(parsed.getEquipment())) {
            bool.filter(termQuery("equipmentRequirement", "NONE"));
            return;
        }
        bool.filter(termsQuery("equipmentRequirement", List.of("NONE", parsed.getEquipment())));
    }

    private String buildVectorText(ParsedRecommendationRequest parsed, String requestText) {
        return Stream.of(
                        requestText,
                        parsed.getGoal(),
                        parsed.getEquipment(),
                        parsed.getPostureType(),
                        parsed.getTargetArea(),
                        BodyAreaMapper.toCanonical(parsed.getTargetArea()),
                        parsed.getImpactLevel(),
                        parsed.isKneeSensitive() ? "knee sensitive" : null,
                        parsed.isBackSensitive() ? "back friendly" : null,
                        parsed.isShoulderSensitive() ? "shoulder mobility rotator cuff shoulder pain recovery" : null
                )
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.joining(" "));
    }

    private Query termQuery(String field, String value) {
        return Query.of(query -> query.term(term -> term.field(field).value(value)));
    }

    private Query termQuery(String field, boolean value) {
        return Query.of(query -> query.term(term -> term.field(field).value(value)));
    }

    private Query termsQuery(String field, List<String> values) {
        return Query.of(query -> query.terms(terms -> terms
                .field(field)
                .terms(fieldTerms -> fieldTerms.value(values.stream().map(FieldValue::of).toList()))
        ));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
