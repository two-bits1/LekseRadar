package com.lekseradar.service.ai;

import com.lekseradar.dto.ai.AiMessageRequestDTO;
import com.lekseradar.dto.ai.AiResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.lekseradar.config.AppConfig;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Клиент OpenAI Chat Completions API (или совместимого прокси через OPENAI_BASE_URL). */
public class OpenAiService implements AiService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    /** Groq/OpenAI при лимите в минуту пишут «Please try again in 3.9075s». */
    private static final Pattern TRY_AGAIN = Pattern.compile("try again in (\\d+(?:\\.\\d+)?)(ms|s)\\b");
    private static final int MAX_RETRIES = 2;
    private static final long MAX_RETRY_WAIT_MS = 10_000;

    private final RestClient http;
    private final String model;

    public OpenAiService(AppConfig.OpenAi cfg) {
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
    public AiResponseDTO complete(List<AiMessageRequestDTO> messages) {
        return call(messages, false);
    }

    @Override
    public AiResponseDTO completeJson(List<AiMessageRequestDTO> messages) {
        return call(messages, true);
    }

    private AiResponseDTO call(List<AiMessageRequestDTO> messages, boolean json) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", json ? 0.4 : 0.7);
        if (json) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        for (int attempt = 1; ; attempt++) {
            try {
                JsonNode res = http.post().uri("/chat/completions").body(body).retrieve().body(JsonNode.class);
                String content = res == null ? null : res.path("choices").path(0).path("message").path("content").asText(null);
                if (content == null || content.isBlank()) {
                    throw new AiServiceException("ИИ вернул пустой ответ");
                }
                JsonNode tokens = res.path("usage").path("total_tokens");
                return new AiResponseDTO(content, res.path("model").asText(model), tokens.isNumber() ? tokens.asInt() : null);
            } catch (RestClientResponseException e) {
                // 401 — неверный ключ, 429 — лимит/нет денег на счету, 404 — неверная модель
                int status = e.getStatusCode().value();
                log.warn("OpenAI HTTP {}: {}", status, e.getResponseBodyAsString());
                long waitMs = status == 429 && attempt <= MAX_RETRIES ? retryAfterMs(e) : -1;
                if (waitMs >= 0) {
                    log.info("Лимит ИИ: повтор через {} мс", waitMs);
                    sleep(waitMs);
                    continue;
                }
                throw new AiServiceException("ИИ временно недоступен (код " + status + "), попробуйте ещё раз");
            } catch (RestClientException e) {
                log.warn("OpenAI request failed: {}", e.getMessage());
                throw new AiServiceException("Не удалось получить ответ от ИИ, попробуйте ещё раз");
            }
        }
    }

    /**
     * Сколько ждать перед повтором после 429: заголовок Retry-After или «try again in 3.9s» в тексте ошибки.
     * Дольше {@link #MAX_RETRY_WAIT_MS} не ждём (-1): это уже дневной лимит или нет денег на счету.
     */
    static long retryAfterMs(RestClientResponseException e) {
        Double seconds = null;
        String header = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("retry-after");
        try {
            if (header != null) seconds = Double.parseDouble(header.strip());
        } catch (NumberFormatException ignored) {
            // ниже — из текста ошибки
        }
        if (seconds == null) {
            Matcher m = TRY_AGAIN.matcher(e.getResponseBodyAsString());
            if (m.find()) seconds = Double.parseDouble(m.group(1)) / ("ms".equals(m.group(2)) ? 1000 : 1);
        }
        if (seconds == null) return -1;
        long ms = (long) Math.ceil(seconds * 1000) + 300;
        return ms <= MAX_RETRY_WAIT_MS ? ms : -1;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("Не удалось получить ответ от ИИ, попробуйте ещё раз");
        }
    }
}
