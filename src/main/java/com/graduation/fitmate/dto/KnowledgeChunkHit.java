package com.graduation.fitmate.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeChunkHit {
    private Long chunkId;
    private Long documentId;
    private String title;
    private String content;
    private String language;
    private String topic;
    private double score;
}
