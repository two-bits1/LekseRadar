package com.lekseradar.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.lekseradar.common.ApiException;
import com.lekseradar.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Клиент OpenAI Chat Completions API. */
public class OpenAiClient implements AiClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final RestClient http;
    private final String model;

    public OpenAiClient(AppProperties.OpenAi cfg) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(10));
        rf.setReadTimeout(Duration.ofSeconds(cfg.timeoutSeconds()));
        this.http = RestClient.builder()
                .baseUrl(cfg.baseUrl())
                // Буферизация, чтобы отправлялся Content-Length (некоторые прокси не принимают chunked)
                .requestFactory(new BufferingClientHttpRequestFactory(rf))
                .defaultHeader("Authorization", "Bearer " + cfg.apiKey())
                .build();
        this.model = cfg.model();
    }

    @Override
    public String complete(List<AiMessage> messages) {
        return call(messages, false);
    }

    @Override
    public String completeJson(List<AiMessage> messages) {
        return call(messages, true);
    }

    @Override
    public String mode() {
        return "openai";
    }

    private String call(List<AiMessage> messages, boolean json) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", json ? 0.4 : 0.7);
        if (json) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        try {
            JsonNode res = http.post().uri("/chat/completions").body(body).retrieve().body(JsonNode.class);
            String content = res == null ? null : res.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "ИИ вернул пустой ответ");
            }
            return content;
        } catch (RestClientException e) {
            log.warn("OpenAI request failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Не удалось получить ответ от ИИ, попробуйте ещё раз");
        }
    }
}
