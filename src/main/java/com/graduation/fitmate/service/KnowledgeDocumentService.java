package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.KnowledgeDocumentForm;
import com.graduation.fitmate.dto.KnowledgeDocumentView;
import com.graduation.fitmate.entity.KnowledgeChunk;
import com.graduation.fitmate.entity.KnowledgeDocument;
import com.graduation.fitmate.mapper.KnowledgeChunkMapper;
import com.graduation.fitmate.mapper.KnowledgeDocumentMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeDocumentService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeChunkingService chunkingService;
    private final KnowledgeIndexService knowledgeIndexService;

    public KnowledgeDocumentService(
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            KnowledgeChunkMapper knowledgeChunkMapper,
            KnowledgeChunkingService chunkingService,
            KnowledgeIndexService knowledgeIndexService
    ) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.chunkingService = chunkingService;
        this.knowledgeIndexService = knowledgeIndexService;
    }

    public List<KnowledgeDocumentView> findAllWithStats() {
        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .orderByDesc(KnowledgeDocument::getUpdatedAt));
        if (documents.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> chunkCounts = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                        .in(KnowledgeChunk::getDocumentId, documents.stream().map(KnowledgeDocument::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(KnowledgeChunk::getDocumentId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
        return documents.stream()
                .map(document -> KnowledgeDocumentView.builder()
                        .document(document)
                        .chunkCount(chunkCounts.getOrDefault(document.getId(), 0))
                        .build())
                .toList();
    }

    public List<KnowledgeChunk> findActiveChunks() {
        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getStatus, "ACTIVE"));
        if (documents.isEmpty()) {
            return List.of();
        }
        return knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .in(KnowledgeChunk::getDocumentId, documents.stream().map(KnowledgeDocument::getId).toList())
                .orderByAsc(KnowledgeChunk::getDocumentId)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
    }

    public KnowledgeDocument findDocument(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    public List<KnowledgeChunk> findChunks(Long documentId) {
        return knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, documentId)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
    }

    @Transactional
    public KnowledgeDocument importDocument(KnowledgeDocumentForm form) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(form.getTitle().trim());
        document.setSourceType(normalize(form.getSourceType(), "GUIDELINE"));
        document.setSourceName(trimToNull(form.getSourceName()));
        document.setLanguage(normalize(form.getLanguage(), "zh_CN"));
        document.setTopic(trimToNull(form.getTopic()));
        document.setTags(trimToNull(form.getTags()));
        document.setStatus("ACTIVE");
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentMapper.insert(document);

        List<String> chunks = chunkingService.chunk(form.getContent());
        for (int index = 0; index < chunks.size(); index++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(index);
            chunk.setContent(chunks.get(index));
            chunk.setTokenCount(chunkingService.estimateTokenCount(chunks.get(index)));
            chunk.setMetadataJson("{}");
            chunk.setIndexed(false);
            chunk.setCreatedAt(LocalDateTime.now());
            chunk.setUpdatedAt(LocalDateTime.now());
            knowledgeChunkMapper.insert(chunk);
            knowledgeIndexService.indexChunk(document, chunk);
        }
        return document;
    }

    @Transactional
    public void archiveDocument(Long id) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(id);
        if (document == null) {
            return;
        }
        document.setStatus("ARCHIVED");
        document.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentMapper.updateById(document);
        knowledgeIndexService.deleteDocumentChunks(id);
    }

    public int reindexAll() {
        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getStatus, "ACTIVE"));
        int indexed = 0;
        for (KnowledgeDocument document : documents) {
            List<KnowledgeChunk> chunks = findChunks(document.getId());
            for (KnowledgeChunk chunk : chunks) {
                knowledgeIndexService.indexChunk(document, chunk);
                indexed++;
            }
        }
        return indexed;
    }

    private String normalize(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
