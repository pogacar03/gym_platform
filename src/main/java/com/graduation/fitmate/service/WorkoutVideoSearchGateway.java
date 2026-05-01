package com.graduation.fitmate.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface WorkoutVideoSearchGateway {

    Map<Long, Double> lexicalSearch(String indexName, int candidateLimit, Query filterQuery, String requestText) throws IOException;

    Map<Long, Double> vectorSearch(
            String indexName,
            int candidateLimit,
            int numCandidates,
            Query filterQuery,
            List<Float> queryVector
    ) throws IOException;
}
