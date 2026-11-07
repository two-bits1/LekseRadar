package com.lekseradar.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.lekseradar.config.AppProperties;
import com.lekseradar.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Клиент OpenAI Chat Completions API (или совместимого прокси через OPENAI_BASE_URL). */
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
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.apiKey())
                .build();
        this.model = cfg.model();
    }

    @Override
    public AiReply complete(List<AiMessage> messages) {
        return call(messages, false);
    }

    @Override
    public AiReply completeJson(List<AiMessage> messages) {
        return call(messages, true);
    }

    private AiReply call(List<AiMessage> messages, boolean json) {
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
            if (content == null || content.isBlank()) {
                throw new AiServiceException("ИИ вернул пустой ответ");
            }
            JsonNode tokens = res.path("usage").path("total_tokens");
            return new AiReply(content, res.path("model").asText(model), tokens.isNumber() ? tokens.asInt() : null);
        } catch (RestClientResponseException e) {
            // 401 — неверный ключ, 429 — лимит/нет денег на счету, 404 — неверная модель
            log.warn("OpenAI HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new AiServiceException("ИИ временно недоступен (код " + e.getStatusCode().value() + "), попробуйте ещё раз");
        } catch (RestClientException e) {
            log.warn("OpenAI request failed: {}", e.getMessage());
            throw new AiServiceException("Не удалось получить ответ от ИИ, попробуйте ещё раз");
        }
    }
}
