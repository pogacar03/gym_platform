package com.graduation.fitmate.dto;

import com.graduation.fitmate.entity.KnowledgeDocument;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeDocumentView {
    private KnowledgeDocument document;
    private int chunkCount;
}
