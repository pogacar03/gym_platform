package com.graduation.fitmate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public record AppLlmProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model
) {
}

