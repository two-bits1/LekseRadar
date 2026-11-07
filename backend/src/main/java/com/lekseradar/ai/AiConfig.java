package com.lekseradar.ai;

import com.lekseradar.config.AppProperties;
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
        if (cfg.hasKey()) {
            log.info("AI: OpenAI, model={}", cfg.model());
            return new OpenAiClient(cfg);
        }
        if (cfg.mockWhenNoKey()) {
            log.warn("AI: OPENAI_API_KEY не задан — используется заглушка MockAiClient");
            return new MockAiClient();
        }
        throw new IllegalStateException("OPENAI_API_KEY не задан, а OPENAI_MOCK=false");
    }
}
