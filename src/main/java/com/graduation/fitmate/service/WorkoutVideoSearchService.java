package com.graduation.fitmate.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.graduation.fitmate.config.AppSearchProperties;
import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.search.WorkoutVideoSearchDocument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkoutVideoSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final AppSearchProperties searchProperties;

    public WorkoutVideoSearchService(ElasticsearchClient elasticsearchClient, AppSearchProperties searchProperties) {
        this.elasticsearchClient = elasticsearchClient;
        this.searchProperties = searchProperties;
    }

    public List<Long> searchCandidateIds(ParsedRecommendationRequest parsed, String requestText, boolean relaxGoal) {
        if (!searchProperties.enabled()) {
            return List.of();
        }

        try {
            BoolQuery.Builder bool = new BoolQuery.Builder()
                    .filter(termQuery("active", true))
                    .filter(termQuery("curated", true));

            if (!relaxGoal && hasText(parsed.getGoal())) {
                bool.filter(termQuery("goal", parsed.getGoal()));
            }
            if (hasText(parsed.getPostureType())) {
                bool.filter(termQuery("postureType", parsed.getPostureType()));
            }
            if (hasText(parsed.getTargetArea())) {
                bool.filter(termsQuery("targetBodyPart", List.of(parsed.getTargetArea(), "FULL_BODY")));
            }
            if (hasText(parsed.getImpactLevel())) {
                bool.filter(termQuery("impactLevel", parsed.getImpactLevel()));
            }

            applyEquipmentRules(bool, parsed);

            if (parsed.isKneeSensitive()) {
                bool.mustNot(termQuery("impactLevel", "HIGH"));
            }
            if (parsed.isBackSensitive()) {
                bool.mustNot(termQuery("targetBodyPart", "CORE"));
            }

            if (hasText(requestText)) {
                bool.should(matchQuery("title", requestText, 3.0f));
                bool.should(matchQuery("description", requestText, 1.5f));
                bool.should(matchQuery("searchText", requestText, 2.0f));
                for (String token : requestTokens(requestText)) {
                    bool.should(matchQuery("extraTags", token, 2.5f));
                }
                bool.minimumShouldMatch("1");
            }

            SearchResponse<WorkoutVideoSearchDocument> response = elasticsearchClient.search(request -> request
                            .index(searchProperties.indexName())
                            .size(Math.max(3, searchProperties.candidateLimit()))
                            .query(Query.of(query -> query.bool(bool.build()))),
                    WorkoutVideoSearchDocument.class
            );

            List<Long> ids = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                WorkoutVideoSearchDocument source = hit.source();
                if (source != null && source.getVideoId() != null) {
                    ids.add(source.getVideoId());
                }
            });
            return ids;
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Elasticsearch search failed, falling back to SQL candidates: {}", ex.getMessage());
            return List.of();
        }
    }

    private void applyEquipmentRules(BoolQuery.Builder bool, ParsedRecommendationRequest parsed) {
        if (!hasText(parsed.getEquipment())) {
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
    private Query matchQuery(String field, String value, float boost) {
        return Query.of(query -> query.match(match -> match.field(field).query(value).boost(boost)));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> requestTokens(String requestText) {
        if (!hasText(requestText)) {
            return List.of();
        }
        return List.of(requestText.toUpperCase(Locale.ROOT).split("[^A-Z_]+")).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .limit(8)
                .toList();
    }
}
