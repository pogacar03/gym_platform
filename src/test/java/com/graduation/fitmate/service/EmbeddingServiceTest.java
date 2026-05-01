package com.graduation.fitmate.service;

import com.graduation.fitmate.config.AppSearchProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    private final EmbeddingService embeddingService = new EmbeddingService(
            new AppSearchProperties(true, "workout_video_search", 8, true, 96, 24, 0.55d, 0.45d, false,
                    "knowledge_chunk_search", 5)
    );

    @Test
    void shouldProduceDeterministicNormalizedEmbeddings() {
        List<Float> first = embeddingService.embed("low impact chair workout for back mobility");
        List<Float> second = embeddingService.embed("low impact chair workout for back mobility");

        assertThat(first).hasSize(96);
        assertThat(second).isEqualTo(first);

        double norm = Math.sqrt(first.stream().mapToDouble(value -> value * value).sum());
        assertThat(norm).isBetween(0.99d, 1.01d);
    }

    @Test
    void shouldReturnZeroVectorForBlankInput() {
        List<Float> embedding = embeddingService.embed("   ");

        assertThat(embedding).hasSize(96);
        assertThat(embedding).allMatch(value -> value == 0.0f);
    }

    @Test
    void shouldProduceNonZeroVectorForChineseInput() {
        List<Float> embedding = embeddingService.embed("我想做低冲击的椅子背部训练");

        assertThat(embedding).hasSize(96);
        assertThat(embedding).anyMatch(value -> value > 0.0f);
    }
}
