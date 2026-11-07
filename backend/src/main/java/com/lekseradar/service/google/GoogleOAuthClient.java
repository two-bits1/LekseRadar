package com.lekseradar.service.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lekseradar.config.AppConfig;
import com.lekseradar.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Вход в Google Classroom (OAuth 2.0, «Web application»): ссылка на страницу Google, обмен кода на токены,
 * обновление access_token по refresh_token. Секрет клиента есть только у бэкенда; collector получает готовый
 * access_token. Токены и коды не логируются.
 */
@Component
public class GoogleOAuthClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

    /** Только чтение: курсы и задания со своими сдачами. */
    static final List<String> SCOPES = List.of(
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.me.readonly");
    static final String NOT_CONFIGURED = "Google Classroom не настроен на сервере (GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET)";
    static final String RECONNECT = "Войдите в Google Classroom заново";
    /** Обновлять access_token, если до конца осталось меньше этого. */
    static final Duration REFRESH_MARGIN = Duration.ofMinutes(2);

    private final String clientId;
    private final String clientSecret;
    private final String authUrl;
    private final String tokenUrl;
    private final RestClient http;
    private final Clock clock;

    @Autowired
    public GoogleOAuthClient(AppConfig cfg) {
        this(cfg.google() == null ? null : cfg.google().clientId(), cfg.google() == null ? null : cfg.google().clientSecret(),
                "https://accounts.google.com/o/oauth2/v2/auth", "https://oauth2.googleapis.com/token", Clock.systemUTC());
    }

    public GoogleOAuthClient(String clientId, String clientSecret, String authUrl, String tokenUrl, Clock clock) {
        this.clientId = clientId == null ? "" : clientId.strip();
        this.clientSecret = clientSecret == null ? "" : clientSecret.strip();
        this.authUrl = authUrl;
        this.tokenUrl = tokenUrl;
        this.clock = clock;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(10));
        rf.setReadTimeout(Duration.ofSeconds(20));
        this.http = RestClient.builder().requestFactory(rf).build();
    }

    public boolean configured() {
        return !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    /**
     * Ссылка на вход Google. state — метка ученика ({@link OAuthStates}); redirectUri — страница сайта, куда Google
     * вернёт ученика (уже проверена: только адреса из CORS_ORIGINS + /settings/platforms).
     */
    public String authorizationUrl(String state, String redirectUri) {
        requireConfigured();
        return UriComponentsBuilder.fromUriString(authUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", SCOPES))
                .queryParam("access_type", "offline")      // нужен refresh_token, чтобы обновлять ДЗ без ученика
                .queryParam("prompt", "consent")           // иначе при повторном входе refresh_token не выдаётся
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .encode().build().toUriString();
    }

    /**
     * Код со страницы возврата → доступы {@code {access_token, refresh_token, expires_at}} (формат OAuthCreds collector'а).
     * @throws ApiException 422 — код не подошёл или ученик снял галочки доступа; 503 — Google или настройки
     */
    public ObjectNode exchange(String code, String redirectUri) {
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");
        JsonNode res = token(form, "exchange");
        String scope = res.path("scope").asText("");
        if (!scope.contains("classroom.courses.readonly")
                || !(scope.contains("classroom.coursework.me") || scope.contains("classroom.student-submissions.me"))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Нужен доступ к курсам и заданиям: при входе в Google отметьте все галочки");
        }
        ObjectNode creds = JsonNodeFactory.instance.objectNode()
                .put("access_token", res.path("access_token").asText())
                .put("expires_at", expiresAt(res));
        if (res.hasNonNull("refresh_token")) creds.put("refresh_token", res.path("refresh_token").asText());
        return creds;
    }

    /**
     * Доступы с живым access_token: если истекает (или срок неизвестен) и есть refresh_token — обновить.
     * Возвращает те же доступы, если обновлять не нужно, иначе новую копию.
     * @throws ApiException 422 {@link #RECONNECT} — Google больше не принимает refresh_token (отозван, 7 дней в режиме Testing)
     */
    public JsonNode fresh(JsonNode creds) {
        long expiresAt = creds.path("expires_at").asLong(0);
        String refresh = creds.path("refresh_token").asText("");
        if (refresh.isEmpty() || expiresAt > clock.instant().plus(REFRESH_MARGIN).getEpochSecond()) {
            return creds;
        }
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refresh);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "refresh_token");
        JsonNode res = token(form, "refresh");
        ObjectNode out = creds.deepCopy();
        out.put("access_token", res.path("access_token").asText());
        out.put("expires_at", expiresAt(res));
        if (res.hasNonNull("refresh_token")) out.put("refresh_token", res.path("refresh_token").asText());
        return out;
    }

    private JsonNode token(MultiValueMap<String, String> form, String what) {
        JsonNode res;
        try {
            res = http.post().uri(tokenUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            String error = "";
            try {
                JsonNode body = e.getResponseBodyAs(JsonNode.class);
                error = body == null ? "" : body.path("error").asText("");
            } catch (RuntimeException ignored) {
                // не JSON
            }
            log.warn("google token {} failed: HTTP {} {}", what, e.getStatusCode().value(), error);
            if ("invalid_grant".equals(error)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "exchange".equals(what) ? "Код входа Google устарел. Нажмите «Подключить Google Classroom» ещё раз" : RECONNECT);
            }
            if ("redirect_uri_mismatch".equals(error)) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Адрес возврата не добавлен в Google Cloud: Authorized redirect URIs → <адрес сайта>/settings/platforms");
            }
            if ("invalid_client".equals(error) || "unauthorized_client".equals(error)) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Неверные GOOGLE_CLIENT_ID или GOOGLE_CLIENT_SECRET");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google не ответил на вход, попробуйте позже");
        } catch (RestClientException e) {
            log.warn("google token {} failed: {}", what, e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google не ответил на вход, попробуйте позже");
        }
        if (res == null || !res.hasNonNull("access_token")) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Google не выдал доступ, попробуйте позже");
        }
        return res;
    }

    private long expiresAt(JsonNode res) {
        return clock.instant().getEpochSecond() + res.path("expires_in").asLong(3600);
    }

    private void requireConfigured() {
        if (!configured()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, NOT_CONFIGURED);
    }
}
