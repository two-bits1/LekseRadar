package com.lekseradar.config;

import com.lekseradar.service.ai.AiClient;
import com.lekseradar.service.ai.OpenAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    AiClient aiClient(AppProperties props) {
        AppProperties.OpenAi cfg = props.openai();
        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "Не задан OPENAI_API_KEY. Впишите его в backend/.env (пример: backend/.env.example) "
                            + "или в переменные окружения");
        }
        log.info("AI: OpenAI, model={}, baseUrl={}", cfg.model(), cfg.baseUrl());
        return new OpenAiClient(cfg);
    }
}
