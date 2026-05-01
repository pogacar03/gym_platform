package com.graduation.fitmate.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeChunkingServiceTest {

    private final KnowledgeChunkingService chunkingService = new KnowledgeChunkingService();

    @Test
    void shouldSplitLongKnowledgeIntoRetrievableChunks() {
        String paragraph = "Low impact chair workouts should keep the feet grounded and avoid rapid twisting. ".repeat(40);

        List<String> chunks = chunkingService.chunk(paragraph);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allMatch(chunk -> chunk.length() <= 900);
        assertThat(chunks.get(0)).contains("Low impact chair workouts");
    }

    @Test
    void shouldKeepShortParagraphsTogether() {
        String content = """
                Knee-sensitive users should avoid jumping.

                Chair support can reduce balance risk.
                """;

        List<String> chunks = chunkingService.chunk(content);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("Knee-sensitive users");
        assertThat(chunks.get(0)).contains("Chair support");
    }
}
