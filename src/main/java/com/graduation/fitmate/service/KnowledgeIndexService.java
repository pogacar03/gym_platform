package com.graduation.fitmate.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.IntegerNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.LongNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.graduation.fitmate.config.AppSearchProperties;
import com.graduation.fitmate.entity.KnowledgeChunk;
import com.graduation.fitmate.entity.KnowledgeDocument;
import com.graduation.fitmate.mapper.KnowledgeChunkMapper;
import com.graduation.fitmate.mapper.KnowledgeDocumentMapper;
import com.graduation.fitmate.search.KnowledgeChunkSearchDocument;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KnowledgeIndexService {

    private final ElasticsearchClient elasticsearchClient;
    private final AppSearchProperties searchProperties;
    private final EmbeddingService embeddingService;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public KnowledgeIndexService(
            ElasticsearchClient elasticsearchClient,
            AppSearchProperties searchProperties,
            EmbeddingService embeddingService,
            KnowledgeChunkMapper knowledgeChunkMapper,
            KnowledgeDocumentMapper knowledgeDocumentMapper
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.searchProperties = searchProperties;
        this.embeddingService = embeddingService;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncActiveKnowledge();
    }

    public boolean ensureIndex() {
        if (!searchProperties.enabled()) {
            return false;
        }
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(request -> request.index(searchProperties.knowledgeIndexName()))
                    .value();
            if (exists) {
                return true;
            }
            elasticsearchClient.indices().create(request -> request
                    .index(searchProperties.knowledgeIndexName())
                    .mappings(mapping -> mapping.properties(indexProperties()))
            );
            log.info("Created Elasticsearch knowledge index {}", searchProperties.knowledgeIndexName());
            return true;
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Could not ensure knowledge index {}: {}", searchProperties.knowledgeIndexName(), ex.getMessage());
            return false;
        }
    }

    public void indexChunk(KnowledgeDocument document, KnowledgeChunk chunk) {
        if (!searchProperties.enabled() || document == null || chunk == null || chunk.getId() == null) {
            return;
        }
        try {
            if (!ensureIndex()) {
                return;
            }
            elasticsearchClient.index(request -> request
                    .index(searchProperties.knowledgeIndexName())
                    .id(String.valueOf(chunk.getId()))
                    .document(toSearchDocument(document, chunk))
            );
            chunk.setIndexed(true);
            chunk.setUpdatedAt(LocalDateTime.now());
            knowledgeChunkMapper.updateById(chunk);
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Could not index knowledge chunk {}: {}", chunk.getId(), ex.getMessage());
        }
    }

    public void deleteDocumentChunks(Long documentId) {
        if (!searchProperties.enabled() || documentId == null) {
            return;
        }
        try {
            if (!ensureIndex()) {
                return;
            }
            elasticsearchClient.deleteByQuery(request -> request
                    .index(searchProperties.knowledgeIndexName())
                    .query(Query.of(query -> query.term(term -> term
                            .field("documentId")
                            .value(documentId)
                    )))
            );
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Could not delete knowledge document {} from Elasticsearch: {}", documentId, ex.getMessage());
        }
    }

    public void syncActiveKnowledge() {
        if (!searchProperties.enabled()) {
            return;
        }
        try {
            if (!ensureIndex()) {
                return;
            }
            List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                    .eq(KnowledgeDocument::getStatus, "ACTIVE"));
            int indexed = 0;
            for (KnowledgeDocument document : documents) {
                List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                        .eq(KnowledgeChunk::getDocumentId, document.getId())
                        .orderByAsc(KnowledgeChunk::getChunkIndex));
                for (KnowledgeChunk chunk : chunks) {
                    indexChunk(document, chunk);
                    indexed++;
                }
            }
            log.info("Synchronized {} knowledge chunks into Elasticsearch index {}", indexed, searchProperties.knowledgeIndexName());
        } catch (Exception ex) {
            log.warn("Knowledge index sync failed: {}", ex.getMessage());
        }
    }

    private KnowledgeChunkSearchDocument toSearchDocument(KnowledgeDocument document, KnowledgeChunk chunk) {
        String searchText = Stream.of(
                        document.getTitle(),
                        document.getSourceName(),
                        document.getTopic(),
                        document.getTags(),
                        chunk.getContent()
                )
                .filter(value -> value != null && !value.isBlank())
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right)
                .trim();
        return KnowledgeChunkSearchDocument.builder()
                .chunkId(chunk.getId())
                .documentId(document.getId())
                .title(document.getTitle())
                .content(chunk.getContent())
                .language(document.getLanguage())
                .topic(document.getTopic())
                .tags(parseTags(document.getTags()))
                .chunkIndex(chunk.getChunkIndex())
                .searchText(searchText)
                .embedding(embeddingService.embed(searchText))
                .build();
    }

    private Map<String, Property> indexProperties() {
        return Map.ofEntries(
                Map.entry("chunkId", Property.of(property -> property.long_(LongNumberProperty.of(number -> number)))),
                Map.entry("documentId", Property.of(property -> property.long_(LongNumberProperty.of(number -> number)))),
                Map.entry("title", Property.of(property -> property.text(TextProperty.of(text -> text)))),
                Map.entry("content", Property.of(property -> property.text(TextProperty.of(text -> text)))),
                Map.entry("language", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("topic", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("tags", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("chunkIndex", Property.of(property -> property.integer(IntegerNumberProperty.of(integer -> integer)))),
                Map.entry("searchText", Property.of(property -> property.text(TextProperty.of(text -> text)))),
                Map.entry("embedding", Property.of(property -> property.denseVector(DenseVectorProperty.of(vector -> vector
                        .dims(searchProperties.embeddingDimensions())
                        .index(searchProperties.vectorEnabled())
                        .similarity("cosine")
                ))))
        );
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Stream.of(tags.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
