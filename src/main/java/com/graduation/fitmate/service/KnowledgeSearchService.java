package com.graduation.fitmate.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.config.AppSearchProperties;
import com.graduation.fitmate.dto.KnowledgeChunkHit;
import com.graduation.fitmate.dto.ParsedRecommendationRequest;
import com.graduation.fitmate.entity.KnowledgeChunk;
import com.graduation.fitmate.entity.KnowledgeDocument;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.KnowledgeChunkMapper;
import com.graduation.fitmate.mapper.KnowledgeDocumentMapper;
import com.graduation.fitmate.search.KnowledgeChunkSearchDocument;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KnowledgeSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final AppSearchProperties searchProperties;
    private final EmbeddingService embeddingService;
    private final KnowledgeIndexService knowledgeIndexService;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    public KnowledgeSearchService(
            ElasticsearchClient elasticsearchClient,
            AppSearchProperties searchProperties,
            EmbeddingService embeddingService,
            KnowledgeIndexService knowledgeIndexService,
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            KnowledgeChunkMapper knowledgeChunkMapper
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.searchProperties = searchProperties;
        this.embeddingService = embeddingService;
        this.knowledgeIndexService = knowledgeIndexService;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
    }

    public List<KnowledgeChunkHit> search(ParsedRecommendationRequest parsed, List<WorkoutVideo> candidates, Locale locale) {
        String queryText = buildQueryText(parsed, candidates);
        if (!hasText(queryText)) {
            return List.of();
        }
        if (!searchProperties.enabled()) {
            return fallbackSearch(queryText, locale);
        }
        try {
            if (!knowledgeIndexService.ensureIndex()) {
                return fallbackSearch(queryText, locale);
            }
            Map<Long, KnowledgeChunkHit> hits = new LinkedHashMap<>();
            for (Hit<KnowledgeChunkSearchDocument> hit : lexicalSearch(queryText, locale).hits().hits()) {
                addHit(hits, hit, 0.62d);
            }
            if (searchProperties.vectorEnabled()) {
                for (Hit<KnowledgeChunkSearchDocument> hit : vectorSearch(queryText, locale).hits().hits()) {
                    addHit(hits, hit, 0.38d);
                }
            }
            return hits.values().stream()
                    .sorted(Comparator.comparingDouble(KnowledgeChunkHit::getScore).reversed())
                    .limit(Math.max(1, searchProperties.knowledgeCandidateLimit()))
                    .toList();
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Knowledge search failed, using database fallback: {}", ex.getMessage());
            return fallbackSearch(queryText, locale);
        }
    }

    private SearchResponse<KnowledgeChunkSearchDocument> lexicalSearch(String queryText, Locale locale) throws IOException {
        BoolQuery.Builder bool = new BoolQuery.Builder()
                .filter(languageFilter(locale))
                .should(matchQuery("title", queryText, 3.0f))
                .should(matchQuery("content", queryText, 2.0f))
                .should(matchQuery("searchText", queryText, 2.5f))
                .minimumShouldMatch("1");
        return elasticsearchClient.search(request -> request
                        .index(searchProperties.knowledgeIndexName())
                        .size(Math.max(1, searchProperties.knowledgeCandidateLimit()))
                        .query(Query.of(query -> query.bool(bool.build()))),
                KnowledgeChunkSearchDocument.class
        );
    }

    private SearchResponse<KnowledgeChunkSearchDocument> vectorSearch(String queryText, Locale locale) throws IOException {
        return elasticsearchClient.search(request -> request
                        .index(searchProperties.knowledgeIndexName())
                        .size(Math.max(1, searchProperties.knowledgeCandidateLimit()))
                        .knn(knn -> knn
                                .field("embedding")
                                .queryVector(embeddingService.embed(queryText))
                                .k((long) Math.max(1, searchProperties.knowledgeCandidateLimit()))
                                .numCandidates((long) Math.max(searchProperties.vectorCandidates(), searchProperties.knowledgeCandidateLimit()))
                                .filter(languageFilter(locale))
                        ),
                KnowledgeChunkSearchDocument.class
        );
    }

    private Query languageFilter(Locale locale) {
        String language = locale != null && locale.getLanguage().toLowerCase(Locale.ROOT).startsWith("zh") ? "zh_CN" : "en";
        return Query.of(query -> query.term(term -> term.field("language").value(language)));
    }

    private void addHit(Map<Long, KnowledgeChunkHit> hits, Hit<KnowledgeChunkSearchDocument> hit, double weight) {
        KnowledgeChunkSearchDocument source = hit.source();
        if (source == null || source.getChunkId() == null) {
            return;
        }
        double score = (hit.score() == null ? 0.0d : hit.score()) * weight;
        hits.merge(source.getChunkId(), toHit(source, score), (left, right) -> {
            left.setScore(left.getScore() + right.getScore());
            return left;
        });
    }

    private KnowledgeChunkHit toHit(KnowledgeChunkSearchDocument source, double score) {
        return KnowledgeChunkHit.builder()
                .chunkId(source.getChunkId())
                .documentId(source.getDocumentId())
                .title(source.getTitle())
                .content(source.getContent())
                .language(source.getLanguage())
                .topic(source.getTopic())
                .score(score)
                .build();
    }

    private List<KnowledgeChunkHit> fallbackSearch(String queryText, Locale locale) {
        String language = locale != null && locale.getLanguage().toLowerCase(Locale.ROOT).startsWith("zh") ? "zh_CN" : "en";
        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getStatus, "ACTIVE")
                .eq(KnowledgeDocument::getLanguage, language));
        if (documents.isEmpty()) {
            return List.of();
        }
        Map<Long, KnowledgeDocument> documentsById = documents.stream()
                .collect(java.util.stream.Collectors.toMap(KnowledgeDocument::getId, document -> document));
        return knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                        .in(KnowledgeChunk::getDocumentId, documentsById.keySet()))
                .stream()
                .map(chunk -> scoreFallbackChunk(queryText, documentsById.get(chunk.getDocumentId()), chunk))
                .filter(hit -> hit.getScore() > 0)
                .sorted(Comparator.comparingDouble(KnowledgeChunkHit::getScore).reversed())
                .limit(Math.max(1, searchProperties.knowledgeCandidateLimit()))
                .toList();
    }

    private KnowledgeChunkHit scoreFallbackChunk(String queryText, KnowledgeDocument document, KnowledgeChunk chunk) {
        String lowerQuery = queryText.toLowerCase(Locale.ROOT);
        String haystack = Stream.of(document.getTitle(), document.getTopic(), document.getTags(), chunk.getContent())
                .filter(this::hasText)
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase(Locale.ROOT);
        double score = Stream.of(lowerQuery.split("[^\\p{L}\\p{N}]+"))
                .filter(this::hasText)
                .filter(haystack::contains)
                .count();
        return KnowledgeChunkHit.builder()
                .chunkId(chunk.getId())
                .documentId(document.getId())
                .title(document.getTitle())
                .content(chunk.getContent())
                .language(document.getLanguage())
                .topic(document.getTopic())
                .score(score)
                .build();
    }

    private String buildQueryText(ParsedRecommendationRequest parsed, List<WorkoutVideo> candidates) {
        return Stream.concat(
                        Stream.of(
                                parsed.getGoal(),
                                parsed.getPostureType(),
                                parsed.getEquipment(),
                                parsed.getTargetArea(),
                                parsed.getImpactLevel(),
                                parsed.isKneeSensitive() ? "knee sensitive knee pain low impact" : null,
                                parsed.isBackSensitive() ? "back friendly back pain spine safe" : null,
                                parsed.isShoulderSensitive() ? "肩部不适 肩周炎 肩痛 shoulder pain frozen shoulder rotator cuff mobility recovery" : null
                        ),
                        candidates.stream().flatMap(video -> Stream.of(video.getTitle(), video.getDescription(), video.getExtraTags()))
                )
                .filter(this::hasText)
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right)
                .trim();
    }

    private Query matchQuery(String field, String value, float boost) {
        return Query.of(query -> query.match(match -> match.field(field).query(value).boost(boost)));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
