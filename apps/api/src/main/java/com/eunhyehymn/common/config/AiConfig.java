package com.eunhyehymn.common.config;

import com.eunhyehymn.application.ports.HymnRecommendationClient;
import com.eunhyehymn.infrastructure.ai.GeminiHymnRecommendationClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    HymnRecommendationClient hymnRecommendationClient(
        ObjectMapper objectMapper,
        @Value("${ai.gemini.enabled:false}") boolean enabled,
        @Value("${ai.gemini.api-key:}") String apiKey,
        @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
        @Value("${ai.gemini.model:gemini-2.5-flash-lite}") String model,
        @Value("${ai.gemini.connect-timeout-seconds:3}") int connectTimeoutSeconds,
        @Value("${ai.gemini.read-timeout-seconds:8}") int readTimeoutSeconds,
        @Value("${ai.gemini.temperature:0.1}") double temperature,
        @Value("${ai.gemini.max-output-tokens:160}") int maxOutputTokens
    ) {
        return new GeminiHymnRecommendationClient(
            objectMapper,
            enabled,
            apiKey,
            baseUrl,
            model,
            connectTimeoutSeconds,
            readTimeoutSeconds,
            temperature,
            maxOutputTokens
        );
    }
}
