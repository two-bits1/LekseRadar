package com.lekseradar.config;

import com.lekseradar.exception.ApiException;
import com.lekseradar.service.ai.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Без OPENAI_API_KEY бэкенд запускается, а ИИ отвечает 503. */
class AiConfigTest {
    @Test
    void withoutKeyAiAnswers503InsteadOfFailingStartup() {
        AppConfig cfg = new AppConfig(null, null, new AppConfig.OpenAi("", "https://api.groq.com/openai/v1", "m", 60),
                null, null, null, null, null);

        AiService ai = new AiConfig().aiClient(cfg);

        ApiException e = assertThrows(ApiException.class, () -> ai.complete(List.of()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
        assertThrows(ApiException.class, () -> ai.completeJson(List.of()));
    }
}
