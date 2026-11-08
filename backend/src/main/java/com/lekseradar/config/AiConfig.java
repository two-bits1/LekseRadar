package com.lekseradar.config;

import com.lekseradar.dto.ai.AiMessageRequestDTO;
import com.lekseradar.dto.ai.AiResponseDTO;
import com.lekseradar.exception.ApiException;
import com.lekseradar.service.ai.AiService;
import com.lekseradar.service.ai.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.List;

@Configuration
public class AiConfig {
    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    AiService aiClient(AppConfig props) {
        AppConfig.OpenAi cfg = props.openai();
        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            // Без ключа сайт всё равно запускается (вход, ДЗ, платформы), а запросы к ИИ отвечают 503
            log.warn("AI: OPENAI_API_KEY не задан — чат, квизы и карточки от ИИ отвечают 503. "
                    + "Впишите ключ в .env (docker compose) или backend/.env");
            return new NotConfigured();
        }
        log.info("AI: OpenAI, model={}, baseUrl={}", cfg.model(), cfg.baseUrl());
        return new OpenAiService(cfg);
    }

    /** ИИ без ключа: каждый вызов — 503 с понятным текстом (фронт показывает «ИИ ещё не настроен»). */
    static final class NotConfigured implements AiService {
        static final String MESSAGE = "ИИ не настроен на сервере: задайте OPENAI_API_KEY";

        @Override
        public AiResponseDTO complete(List<AiMessageRequestDTO> messages) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, MESSAGE);
        }

        @Override
        public AiResponseDTO completeJson(List<AiMessageRequestDTO> messages) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, MESSAGE);
        }
    }
}
