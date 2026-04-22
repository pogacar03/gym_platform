package com.graduation.fitmate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.search")
public record AppSearchProperties(
        boolean enabled,
        String indexName,
        int candidateLimit
) {
}
