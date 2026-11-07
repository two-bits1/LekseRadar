package com.lekseradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Настройки приложения из секции {@code app:} в application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppConfig(Jwt jwt, Cors cors, OpenAi openai, Credentials credentials, Collector collector,
                        Platforms platforms, Mail mail) {

    public record Jwt(String secret, long ttlHours) {}

    public record Cors(List<String> allowedOrigins) {}

    public record OpenAi(String apiKey, String baseUrl, String model, int timeoutSeconds) {}

    /** Ключ AES-256 для доступов к платформам, base64 (CREDENTIALS_KEY). */
    public record Credentials(String key) {}

    /** Сервис collector (homework-collector): адрес и общий ключ X-Internal-Key. */
    public record Collector(String url, String key, int timeoutSeconds) {}

    /** Свои Canvas-серверы вне *.instructure.com (CANVAS_HOSTS через запятую, например canvas.uio.no). */
    public record Platforms(List<String> extraCanvasHosts) {}

    /**
     * Письма с кодом подтверждения (SMTP в spring.mail.*). from — адрес отправителя, по умолчанию MAIL_USERNAME.
     * devLogCodes — без настроенной почты печатать код в лог вместо письма (только для разработки).
     */
    public record Mail(String from, boolean devLogCodes) {}
}
