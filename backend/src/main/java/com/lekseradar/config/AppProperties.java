package com.lekseradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, OpenAi openai) {

    public record Jwt(String secret, long ttlHours) {}

    public record Cors(List<String> allowedOrigins) {}

    public record OpenAi(String apiKey, String baseUrl, String model, boolean mockWhenNoKey,
                         int timeoutSeconds, int chatHistoryLimit) {
        public boolean hasKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
