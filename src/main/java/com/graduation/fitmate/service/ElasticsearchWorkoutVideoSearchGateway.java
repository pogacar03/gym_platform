package com.graduation.fitmate.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.graduation.fitmate.search.WorkoutVideoSearchDocument;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchWorkoutVideoSearchGateway implements WorkoutVideoSearchGateway {

    private final ElasticsearchClient elasticsearchClient;

    public ElasticsearchWorkoutVideoSearchGateway(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public Map<Long, Double> lexicalSearch(String indexName, int candidateLimit, Query filterQuery, String requestText)
            throws IOException {
        BoolQuery.Builder bool = new BoolQuery.Builder().filter(filterQuery);
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
                        .index(indexName)
                        .size(Math.max(3, candidateLimit))
                        .query(Query.of(query -> query.bool(bool.build()))),
                WorkoutVideoSearchDocument.class
        );
        return collectScores(response.hits().hits());
    }

    @Override
    public Map<Long, Double> vectorSearch(
            String indexName,
            int candidateLimit,
            int numCandidates,
            Query filterQuery,
            List<Float> queryVector
    ) throws IOException {
        SearchResponse<WorkoutVideoSearchDocument> response = elasticsearchClient.search(request -> request
                        .index(indexName)
                        .size(Math.max(3, candidateLimit))
                        .knn(knn -> knn
                                .field("embedding")
                                .queryVector(queryVector)
                                .k((long) Math.max(3, candidateLimit))
                                .numCandidates((long) Math.max(numCandidates, candidateLimit))
                                .filter(filterQuery)
                        ),
                WorkoutVideoSearchDocument.class
        );
        return collectScores(response.hits().hits());
    }

    private Map<Long, Double> collectScores(List<Hit<WorkoutVideoSearchDocument>> hits) {
        Map<Long, Double> scores = new LinkedHashMap<>();
        for (Hit<WorkoutVideoSearchDocument> hit : hits) {
            WorkoutVideoSearchDocument source = hit.source();
            if (source == null || source.getVideoId() == null) {
                continue;
            }
            scores.put(source.getVideoId(), hit.score() == null ? 0.0d : hit.score());
        }
        return scores;
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
        return Stream.of(requestText.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }
}
