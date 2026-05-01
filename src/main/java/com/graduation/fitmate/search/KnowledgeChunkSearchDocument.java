package com.graduation.fitmate.search;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkSearchDocument {
    private Long chunkId;
    private Long documentId;
    private String title;
    private String content;
    private String language;
    private String topic;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private Integer chunkIndex;
    private String searchText;
    @Builder.Default
    private List<Float> embedding = new ArrayList<>();
}
