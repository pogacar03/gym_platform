package com.graduation.fitmate.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(AppLlmProperties.class)
@EnableScheduling
public class AppConfig {
}
