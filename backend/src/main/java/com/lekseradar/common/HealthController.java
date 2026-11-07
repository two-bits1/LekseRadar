package com.lekseradar.common;

import com.lekseradar.ai.AiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    private final AiClient ai;

    public HealthController(AiClient ai) {
        this.ai = ai;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "aiMode", ai.mode());
    }
}
