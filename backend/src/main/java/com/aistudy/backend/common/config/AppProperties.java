package com.aistudy.backend.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Ai ai,
        Rag rag,
        Storage storage) {
    public record Jwt(String secret, long expirationMs) {}
    public record Ai(String apiKey, String baseUrl, String chatModel, String embeddingModel,
                     int embeddingDimensions, long timeoutMs, boolean offlineFallbackEnabled) {}
    public record Rag(int topK) {}
    public record Storage(String location) {}
}
