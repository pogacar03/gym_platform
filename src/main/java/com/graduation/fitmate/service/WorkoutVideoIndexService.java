package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty;
import co.elastic.clients.elasticsearch._types.mapping.IntegerNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.graduation.fitmate.config.AppSearchProperties;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.WorkoutVideoMapper;
import com.graduation.fitmate.search.WorkoutVideoSearchDocument;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkoutVideoIndexService {

    private final ElasticsearchClient elasticsearchClient;
    private final AppSearchProperties searchProperties;
    private final WorkoutVideoMapper workoutVideoMapper;
    private final EmbeddingService embeddingService;

    public WorkoutVideoIndexService(
            ElasticsearchClient elasticsearchClient,
            AppSearchProperties searchProperties,
            WorkoutVideoMapper workoutVideoMapper,
            EmbeddingService embeddingService
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.searchProperties = searchProperties;
        this.workoutVideoMapper = workoutVideoMapper;
        this.embeddingService = embeddingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!searchProperties.enabled()) {
            log.info("Search indexing is disabled.");
            return;
        }
        syncAllActiveVideos();
    }

    public boolean ensureIndex() {
        if (!searchProperties.enabled()) {
            return false;
        }
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(request -> request.index(searchProperties.indexName()))
                    .value();
            if (exists) {
                return true;
            }
            elasticsearchClient.indices().create(request -> request
                    .index(searchProperties.indexName())
                    .mappings(mapping -> mapping.properties(indexProperties()))
            );
            log.info("Created Elasticsearch index {}", searchProperties.indexName());
            return true;
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Could not ensure Elasticsearch index {}: {}", searchProperties.indexName(), ex.getMessage());
            return false;
        }
    }

    public void syncAllActiveVideos() {
        if (!searchProperties.enabled()) {
            return;
        }
        try {
            boolean ready = searchProperties.rebuildOnStartup() ? rebuildIndex() : ensureIndex();
            if (!ready) {
                return;
            }
            List<WorkoutVideo> activeVideos = workoutVideoMapper.selectList(new LambdaQueryWrapper<WorkoutVideo>()
                    .eq(WorkoutVideo::getActive, true));
            Set<String> activeIds = new HashSet<>();
            for (WorkoutVideo video : activeVideos) {
                activeIds.add(String.valueOf(video.getId()));
                indexDocument(video);
            }
            pruneInactiveDocuments(activeIds);
            log.info("Synchronized {} active videos into Elasticsearch index {}",
                    activeVideos.size(), searchProperties.indexName());
        } catch (Exception ex) {
            log.warn("Bulk Elasticsearch sync failed: {}", ex.getMessage());
        }
    }

    public void indexVideo(WorkoutVideo video) {
        if (!searchProperties.enabled() || video == null || video.getId() == null) {
            return;
        }
        try {
            if (!ensureIndex()) {
                return;
            }
            indexDocument(video);
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Could not index workout video {} into Elasticsearch: {}", video.getId(), ex.getMessage());
        }
    }

    private boolean rebuildIndex() {
        if (!searchProperties.enabled()) {
            return false;
        }
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(request -> request.index(searchProperties.indexName()))
                    .value();
            if (exists) {
                elasticsearchClient.indices().delete(request -> request.index(searchProperties.indexName()));
            }
            elasticsearchClient.indices().create(request -> request
                    .index(searchProperties.indexName())
                    .mappings(mapping -> mapping.properties(indexProperties()))
            );
            log.info("Rebuilt Elasticsearch index {}", searchProperties.indexName());
            return true;
        } catch (IOException | ElasticsearchException ex) {
            log.warn("Could not rebuild Elasticsearch index {}: {}", searchProperties.indexName(), ex.getMessage());
            return false;
        }
    }

    private void indexDocument(WorkoutVideo video) throws IOException {
        elasticsearchClient.index(request -> request
                .index(searchProperties.indexName())
                .id(String.valueOf(video.getId()))
                .document(toDocument(video))
        );
    }

    private void pruneInactiveDocuments(Set<String> activeIds) throws IOException {
        SearchResponse<WorkoutVideoSearchDocument> response = elasticsearchClient.search(request -> request
                        .index(searchProperties.indexName())
                        .size(10_000),
                WorkoutVideoSearchDocument.class
        );
        int removed = 0;
        for (var hit : response.hits().hits()) {
            if (hit.id() == null || activeIds.contains(hit.id())) {
                continue;
            }
            elasticsearchClient.delete(request -> request
                    .index(searchProperties.indexName())
                    .id(hit.id())
            );
            removed++;
        }
        if (removed > 0) {
            log.info("Removed {} stale Elasticsearch documents from index {}", removed, searchProperties.indexName());
        }
    }

    private WorkoutVideoSearchDocument toDocument(WorkoutVideo video) {
        return WorkoutVideoSearchDocument.builder()
                .videoId(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .goal(video.getTargetGoal())
                .targetBodyPart(video.getTargetBodyPart())
                .equipmentRequirement(video.getEquipmentRequirement())
                .postureType(video.getPostureType())
                .impactLevel(video.getImpactLevel())
                .difficulty(video.getDifficulty())
                .extraTags(parseExtraTags(video.getExtraTags()))
                .platformChannel(video.getPlatformChannel())
                .durationMinutes(video.getDurationMinutes())
                .active(Boolean.TRUE.equals(video.getActive()))
                .curated(Boolean.TRUE.equals(video.getCurated()))
                .searchText(buildSearchText(video))
                .embedding(embeddingService.embed(buildSearchText(video)))
                .build();
    }

    private Map<String, Property> indexProperties() {
        return Map.ofEntries(
                Map.entry("videoId", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("title", Property.of(property -> property.text(TextProperty.of(text -> text)))),
                Map.entry("description", Property.of(property -> property.text(TextProperty.of(text -> text)))),
                Map.entry("goal", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("targetBodyPart", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("equipmentRequirement", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("postureType", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("impactLevel", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("difficulty", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("extraTags", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("platformChannel", Property.of(property -> property.keyword(KeywordProperty.of(keyword -> keyword)))),
                Map.entry("durationMinutes", Property.of(property -> property.integer(IntegerNumberProperty.of(integer -> integer)))),
                Map.entry("active", Property.of(property -> property.boolean_(BooleanProperty.of(bool -> bool)))),
                Map.entry("curated", Property.of(property -> property.boolean_(BooleanProperty.of(bool -> bool)))),
                Map.entry("searchText", Property.of(property -> property.text(TextProperty.of(text -> text)))),
                Map.entry("embedding", Property.of(property -> property.denseVector(DenseVectorProperty.of(vector -> vector
                        .dims(searchProperties.embeddingDimensions())
                        .index(searchProperties.vectorEnabled())
                        .similarity("cosine")
                ))))
        );
    }

    private List<String> parseExtraTags(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Stream.of(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String buildSearchText(WorkoutVideo video) {
        return Stream.of(
                        video.getTitle(),
                        video.getDescription(),
                        video.getTargetGoal(),
                        video.getTargetBodyPart(),
                        video.getEquipmentRequirement(),
                        video.getPostureType(),
                        video.getImpactLevel(),
                        video.getDifficulty(),
                        video.getExtraTags(),
                        video.getPlatformChannel()
                )
                .filter(value -> value != null && !value.isBlank())
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right)
                .trim();
    }
}
