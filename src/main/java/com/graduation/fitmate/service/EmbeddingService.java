package com.graduation.fitmate.service;

import com.graduation.fitmate.config.AppSearchProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private final AppSearchProperties searchProperties;

    public EmbeddingService(AppSearchProperties searchProperties) {
        this.searchProperties = searchProperties;
    }

    public List<Float> embed(String text) {
        int dimensions = Math.max(8, searchProperties.embeddingDimensions());
        float[] values = new float[dimensions];
        if (text == null || text.isBlank()) {
            return zeros(dimensions);
        }

        String normalized = text.toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            int bucket = Math.floorMod(token.hashCode(), dimensions);
            float weight = 1.0f + Math.min(token.length(), 12) / 12.0f;
            values[bucket] += weight;
        }

        float norm = 0.0f;
        for (float value : values) {
            norm += value * value;
        }
        if (norm == 0.0f) {
            return zeros(dimensions);
        }
        float scale = (float) (1.0 / Math.sqrt(norm));
        List<Float> embedding = new ArrayList<>(dimensions);
        for (float value : values) {
            embedding.add(value * scale);
        }
        return embedding;
    }

    private List<Float> zeros(int dimensions) {
        List<Float> values = new ArrayList<>(dimensions);
        for (int index = 0; index < dimensions; index++) {
            values.add(0.0f);
        }
        return values;
    }
}
