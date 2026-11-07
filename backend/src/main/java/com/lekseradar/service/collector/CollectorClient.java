package com.lekseradar.service.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lekseradar.config.AppConfig;
import com.lekseradar.exception.ApiException;
import com.lekseradar.model.enums.PlatformEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * HTTP-клиент к сервису {@code collector} (homework-collector, контракт {@code homework-collector/COLLECTOR-API.md}).
 * Тела запросов и ответов не логируются: в них токены платформ.
 */
@Component
public class CollectorClient {
    private static final Logger log = LoggerFactory.getLogger(CollectorClient.class);
    static final String UNAVAILABLE = "Сервис сбора ДЗ недоступен";

    private final RestClient http;
    private final String baseUrl;

    @Autowired
    public CollectorClient(AppConfig cfg) {
        this(cfg.collector().url(), cfg.collector().key(), Duration.ofSeconds(cfg.collector().timeoutSeconds()));
    }

    public CollectorClient(String baseUrl, String key, Duration timeout) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(5));
        rf.setReadTimeout(timeout);
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                // Content-Length вместо chunked: не все прокси и серверы принимают chunked
                .requestFactory(new BufferingClientHttpRequestFactory(rf))
                .defaultHeader("X-Internal-Key", key == null ? "" : key)
                .build();
    }

    /** Итог одного {@code POST /collect}. */
    public record CollectResult(JsonNode assignments, Map<String, Source> sources,
                                Map<String, JsonNode> updatedCredentials) {
        public Source source(PlatformEnum p) { return sources.get(p.value()); }
    }

    public record Source(boolean ok, int count, String error, boolean needsReconnect) {}

    public enum Status { OK, UNREACHABLE, WRONG_KEY }

    /** Есть ли связь с collector и подходит ли ключ. Пустой {@code POST /collect} ничего не собирает. */
    public Status check() {
        try {
            http.post().uri("/collect").body(Map.of()).retrieve().toBodilessEntity();
            return Status.OK;
        } catch (RestClientResponseException e) {
            return e.getStatusCode().value() == 401 ? Status.WRONG_KEY : Status.UNREACHABLE;
        } catch (RestClientException e) {
            return Status.UNREACHABLE;
        }
    }

    /**
     * При старте сразу пишет в лог, будет ли работать подключение платформ: без collector ученик вписывает ключи
     * на сайте, а ДЗ в базу не попадают. Отдельный поток, чтобы не задерживать запуск.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logStatusOnStartup() {
        Thread.ofVirtual().name("collector-check").start(() -> {
            switch (check()) {
                case OK -> log.info("collector {}: связь есть, ключ подходит — ДЗ с платформ будут сохраняться", baseUrl);
                case WRONG_KEY -> log.warn("collector {} не принял COLLECTOR_KEY: платформы не подключатся и ДЗ не попадут в базу. "
                        + "Ключ у бэкенда и у collector должен быть одинаковым", baseUrl);
                case UNREACHABLE -> log.warn("collector {} недоступен: платформы не подключатся и ДЗ не попадут в базу. "
                        + "Запустите его: cd database && docker compose up -d collector", baseUrl);
            }
        });
    }

    /** Собрать ДЗ со всех переданных платформ. Ошибки платформ не бросаются, они в {@code sources}. */
    public CollectResult collect(Map<PlatformEnum, JsonNode> credentials) {
        Map<String, JsonNode> body = new LinkedHashMap<>();
        credentials.forEach((p, c) -> body.put(p.value(), c));
        JsonNode res = call("collect", () -> http.post().uri("/collect").body(body).retrieve().body(JsonNode.class));

        Map<String, Source> sources = new LinkedHashMap<>();
        res.path("sources").properties().forEach(e -> {
            JsonNode s = e.getValue();
            sources.put(e.getKey(), new Source(s.path("ok").asBoolean(false), s.path("count").asInt(0),
                    s.path("error").isTextual() ? s.path("error").asText() : null,
                    s.path("needs_reconnect").asBoolean(false)));
        });
        Map<String, JsonNode> updated = new LinkedHashMap<>();
        res.path("updated_credentials").properties().forEach(e -> {
            if (e.getValue().isObject()) updated.put(e.getKey(), e.getValue());
        });
        JsonNode assignments = res.path("assignments").isArray() ? res.path("assignments")
                : JsonNodeFactory.instance.arrayNode();
        return new CollectResult(assignments, sources, updated);
    }

    /** Вход в Teams по коду устройства: {@code user_code, verification_uri, device_code, interval, expires_in}. */
    public JsonNode teamsStart() {
        return call("teams/start", () -> http.post().uri("/login/teams/start").retrieve().body(JsonNode.class));
    }

    /** Пусто — ученик ещё не ввёл код; иначе доступы Teams ({@code TeamsCreds}). */
    public Optional<JsonNode> teamsPoll(String deviceCode) {
        ObjectNode body = JsonNodeFactory.instance.objectNode().put("device_code", deviceCode);
        JsonNode res = call("teams/poll", () -> http.post().uri("/login/teams/poll").body(body)
                .retrieve().body(JsonNode.class));
        return res.path("pending").asBoolean(false) ? Optional.empty() : Optional.of(res);
    }

    /** Ссылка, по которой ученик входит в itslearning. */
    public String itslearningUrl(String baseUrl) {
        JsonNode res = call("itslearning/url", () -> http.get()
                .uri(u -> u.path("/login/itslearning/url").queryParam("base_url", baseUrl).build())
                .retrieve().body(JsonNode.class));
        String url = res.path("url").asText("");
        if (url.isBlank()) throw unavailable("itslearning/url", "no url");
        return url;
    }

    /** Код (или вся ссылка с кодом) после входа → доступы itslearning ({@code ItslearningCreds}). */
    public JsonNode itslearningExchange(String baseUrl, String codeOrUrl) {
        ObjectNode body = JsonNodeFactory.instance.objectNode().put("base_url", baseUrl).put("code_or_url", codeOrUrl);
        return call("itslearning/exchange", () -> http.post().uri("/login/itslearning/exchange").body(body)
                .retrieve().body(JsonNode.class));
    }

    /**
     * По COLLECTOR-API.md: 400 — платформа не приняла вход ({@code SourceError}) → 422 с текстом от collector;
     * 502 — платформа недоступна при входе → 502 с текстом. Всё остальное (нет связи, 401 из-за неверного
     * COLLECTOR_KEY, 422 от FastAPI, 5xx) → 503. 401 наружу не отдаём никогда: фронт на 401 выкидывает ученика на вход.
     */
    private JsonNode call(String what, Supplier<JsonNode> request) {
        JsonNode res;
        try {
            res = request.get();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 400) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, detail(e, "Платформа не приняла вход"));
            }
            if (status == 502) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, detail(e, "Платформа сейчас недоступна, попробуйте позже"));
            }
            throw unavailable(what, "HTTP " + status);
        } catch (RestClientException e) {
            throw unavailable(what, e.getClass().getSimpleName());
        }
        if (res == null || !res.isObject()) throw unavailable(what, "empty response");
        return res;
    }

    /** Текст {@code detail} из ответа collector (у FastAPI-ошибок валидации это список — тогда общий текст). */
    private static String detail(RestClientResponseException e, String fallback) {
        try {
            JsonNode body = e.getResponseBodyAs(JsonNode.class);
            JsonNode d = body == null ? null : body.path("detail");
            return d != null && d.isTextual() && !d.asText().isBlank() ? d.asText() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;   // тело не JSON
        }
    }

    private ApiException unavailable(String what, String reason) {
        log.warn("collector {} failed: {}", what, reason);
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }
}
