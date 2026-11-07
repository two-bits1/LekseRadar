package com.lekseradar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lekseradar.config.AppConfig;
import com.lekseradar.dto.platform.PlatformResponseDTO;
import com.lekseradar.dto.platform.TeamsPollResponseDTO;
import com.lekseradar.dto.platform.TeamsStartResponseDTO;
import com.lekseradar.exception.ApiException;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.model.PlatformConnectionEntity;
import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.repository.PlatformConnectionRepository;
import com.lekseradar.security.CredentialsCipher;
import com.lekseradar.security.RateLimiter;
import com.lekseradar.service.collector.CollectorClient;
import com.lekseradar.service.collector.CollectorClient.CollectResult;
import com.lekseradar.service.collector.CollectorClient.Source;
import com.lekseradar.service.google.GoogleOAuthClient;
import com.lekseradar.service.google.OAuthStates;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Подключение платформ ({@code POST /platforms/{platform}}): тело — доступы в формате homework-collector (snake_case),
 * как их собирает фронт. Проверить адрес (SSRF) → проверить доступ одним collect() → сохранить зашифрованным →
 * сразу записать собранные ДЗ. 401 отсюда не отдаётся никогда (фронт на 401 разлогинивает): доступ не принят — 422.
 */
@Service
public class PlatformService {
    static final int CONNECT_LIMIT = 10;
    static final Duration CONNECT_WINDOW = Duration.ofMinutes(10);
    static final String BAD_ADDRESS = "Адрес платформы не поддерживается";

    /** Сервер ходит на адрес ученика только внутри этих доменов (поддомены). */
    static final List<String> CANVAS_DOMAINS = List.of("instructure.com");
    static final List<String> ITSLEARNING_DOMAINS = List.of("itslearning.com");
    static final List<String> INSCHOOL_DOMAINS = List.of("inschool.visma.no");
    private static final Pattern HOST = Pattern.compile("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+");
    private static final Pattern COOKIE_NAME = Pattern.compile("[A-Za-z0-9_.\\-]{1,100}");
    private static final Pattern GUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final int MAX_URL = 300;
    private static final int MAX_SECRET = 10_000;   // токены Teams (JWT) и cookie InSchool бывают длинными
    private static final int MAX_COOKIES = 30;

    private final PlatformConnectionRepository connections;
    private final CollectorClient collector;
    private final CredentialsCipher cipher;
    private final HomeworkSyncService sync;
    private final RateLimiter limiter;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    private final Set<String> extraCanvasHosts;
    private final GoogleOAuthClient google;
    private final OAuthStates states;
    /** Куда Google может вернуть ученика: каждый сайт из CORS_ORIGINS + /settings/platforms (там страница фронта). */
    private final Set<String> classroomRedirects;
    static final String CLASSROOM_PATH = "/settings/platforms";

    public PlatformService(PlatformConnectionRepository connections, CollectorClient collector, CredentialsCipher cipher,
                           HomeworkSyncService sync, RateLimiter limiter, ObjectMapper json,
                           PlatformTransactionManager txManager, AppConfig cfg, GoogleOAuthClient google,
                           OAuthStates states) {
        this.connections = connections;
        this.collector = collector;
        this.cipher = cipher;
        this.sync = sync;
        this.limiter = limiter;
        this.json = json;
        this.tx = new TransactionTemplate(txManager);
        this.google = google;
        this.states = states;
        List<String> origins = cfg.cors() == null ? null : cfg.cors().allowedOrigins();
        this.classroomRedirects = origins == null ? Set.of() : origins.stream()
                .map(o -> o.strip().replaceAll("/+$", "")).filter(o -> !o.isEmpty())
                .map(o -> o + CLASSROOM_PATH).collect(Collectors.toSet());
        List<String> extra = cfg.platforms() == null ? null : cfg.platforms().extraCanvasHosts();
        this.extraCanvasHosts = extra == null ? Set.of() : extra.stream()
                .map(h -> h.strip().toLowerCase(Locale.ROOT)).filter(h -> !h.isEmpty()).collect(Collectors.toSet());
    }

    /** Всегда 4 платформы: подключённые и нет. */
    public List<PlatformResponseDTO> list(Long userId) {
        Map<PlatformEnum, PlatformConnectionEntity> mine = connections.findByUserIdOrderByPlatform(userId).stream()
                .collect(Collectors.toMap(PlatformConnectionEntity::getPlatform, Function.identity()));
        return Arrays.stream(PlatformEnum.values())
                .map(p -> mine.containsKey(p) ? PlatformResponseDTO.from(mine.get(p)) : PlatformResponseDTO.notConnected(p))
                .toList();
    }

    /**
     * Подключить (или обновить) платформу. Тело по платформе:
     * <ul>
     *   <li>canvas: {@code {"base_url": "https://skole.instructure.com", "token": "…"}}</li>
     *   <li>inschool: {@code {"host": "skole.inschool.visma.no", "cookies": {"JSESSIONID": "…"}}}</li>
     *   <li>teams: {@code {"access_token": "…", "refresh_token": "…"}} (хотя бы один)</li>
     *   <li>itslearning: {@code {"base_url": "https://sms.itslearning.com", "refresh_token": "…"}}
     *       или {@code {"base_url": "…", "code_or_url": "itsl-itslearning://login?code=…"}}</li>
     * </ul>
     * В collector уходят только эти поля; пустые строки считаются «не указано».
     */
    public PlatformResponseDTO connect(Long userId, PlatformEnum platform, JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new BadRequestException("Тело запроса — JSON-объект с доступами платформы");
        }
        if (platform == PlatformEnum.CLASSROOM) return connectClassroom(userId, body);
        String code = platform == PlatformEnum.ITSLEARNING ? text(body, "code_or_url", MAX_SECRET) : null;
        ObjectNode creds = switch (platform) {
            case CANVAS -> canvas(body);
            case INSCHOOL -> inschool(body);
            case TEAMS -> tokens(body, json.createObjectNode(), true);
            case ITSLEARNING -> {
                ObjectNode c = json.createObjectNode()
                        .put("base_url", allowedUrl(required(body, "base_url", MAX_URL), ITSLEARNING_DOMAINS, Set.of()));
                yield code != null ? c : tokens(body, c, false);
            }
            case CLASSROOM -> throw new IllegalStateException("выше");
        };
        limit(userId);
        // Код или ссылка после входа в itslearning → токены выдаёт collector
        JsonNode toCheck = code != null ? collector.itslearningExchange(creds.path("base_url").asText(), code) : creds;
        return verifyAndSave(userId, platform, toCheck);
    }

    /** Вход в Teams по коду устройства (по желанию, вместо ручного ввода токенов). */
    public TeamsStartResponseDTO teamsStart(Long userId) {
        limit(userId);
        JsonNode r = collector.teamsStart();
        return new TeamsStartResponseDTO(r.path("user_code").asText(), r.path("verification_uri").asText(),
                r.path("device_code").asText(), r.path("interval").asInt(5), r.path("expires_in").asInt(900));
    }

    /** Фронт опрашивает раз в interval секунд; лимит считается, только когда вход состоялся. */
    public TeamsPollResponseDTO teamsPoll(Long userId, String deviceCode) {
        Optional<JsonNode> creds = collector.teamsPoll(deviceCode.strip());
        if (creds.isEmpty()) return TeamsPollResponseDTO.pending();
        limit(userId);
        return TeamsPollResponseDTO.connected(verifyAndSave(userId, PlatformEnum.TEAMS, creds.get()));
    }

    /** Ссылка для входа в itslearning (по желанию): после входа ученик вставляет code_or_url. */
    public String itslearningUrl(String baseUrl) {
        return collector.itslearningUrl(allowedUrl(baseUrl, ITSLEARNING_DOMAINS, Set.of()));
    }

    /**
     * Google Classroom, шаг 1: ссылка на вход Google. После входа Google вернёт ученика на {@code redirectUri}
     * ({@code <сайт>/settings/platforms}) с {@code ?code=…&state=…}, фронт отправит их в {@code POST /platforms/classroom}.
     */
    public String classroomUrl(Long userId, String redirectUri) {
        String redirect = classroomRedirect(redirectUri);
        if (!google.configured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Classroom не настроен на сервере (GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET)");
        }
        return google.authorizationUrl(states.issue(userId), redirect);
    }

    /**
     * Google Classroom, шаг 2: {@code {"code", "state", "redirect_uri"}} со страницы возврата → токены → проверка → сохранить.
     * redirect_uri — тот же адрес, что был в ссылке входа (Google это сверяет).
     */
    private PlatformResponseDTO connectClassroom(Long userId, JsonNode body) {
        String code = required(body, "code", 2000);
        String state = required(body, "state", 500);
        String redirect = classroomRedirect(required(body, "redirect_uri", MAX_URL));
        if (!states.valid(state, userId)) {
            throw new BadRequestException("Ссылка для входа устарела или уже использована. Нажмите «Подключить Google Classroom» ещё раз");
        }
        if (!connections.platformSupported(PlatformEnum.CLASSROOM.value())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google Classroom ещё не включён в базе (нужна миграция V22)");
        }
        limit(userId);
        return verifyAndSave(userId, PlatformEnum.CLASSROOM, google.exchange(code, redirect));
    }

    /** Только точное совпадение с сайтом из CORS_ORIGINS + /settings/platforms, иначе это был бы открытый редирект. */
    private String classroomRedirect(String raw) {
        String r = raw == null ? "" : raw.strip();
        if (!classroomRedirects.contains(r)) {
            throw new BadRequestException("redirect_uri не разрешён: только адрес сайта из CORS_ORIGINS + " + CLASSROOM_PATH);
        }
        return r;
    }

    /** ДЗ этой платформы остаются в базе. */
    public void disconnect(Long userId, PlatformEnum platform) {
        connections.findByUserIdAndPlatform(userId, platform).ifPresent(connections::delete);
    }

    private ObjectNode canvas(JsonNode body) {
        String url = allowedUrl(required(body, "base_url", MAX_URL), CANVAS_DOMAINS, extraCanvasHosts);
        return json.createObjectNode().put("base_url", url).put("token", required(body, "token", 500));
    }

    private ObjectNode inschool(JsonNode body) {
        String raw = required(body, "host", MAX_URL);   // collector ждёт имя хоста; принимаем и ссылку https://…
        String host = hostOf(raw.contains("://") ? raw : "https://" + raw, INSCHOOL_DOMAINS, Set.of());
        JsonNode cookies = body.get("cookies");
        if (cookies == null || !cookies.isObject()) {
            throw new BadRequestException("cookies: объект с cookie сессии InSchool, например {\"JSESSIONID\": \"…\"}");
        }
        ObjectNode out = json.createObjectNode();
        for (Map.Entry<String, JsonNode> e : cookies.properties()) {
            String value = text(cookies, e.getKey(), MAX_SECRET);
            if (value == null) continue;   // пустую cookie (например, Authorization) просто не отправляем
            if (!COOKIE_NAME.matcher(e.getKey()).matches()) {
                throw new BadRequestException("cookies: недопустимое имя cookie");
            }
            out.put(e.getKey(), value);
        }
        if (out.isEmpty()) throw new BadRequestException("Нужна хотя бы одна cookie сессии InSchool (JSESSIONID)");
        if (out.size() > MAX_COOKIES) throw new BadRequestException("cookies: слишком много cookie");
        ObjectNode c = json.createObjectNode().put("host", host);
        c.set("cookies", out);
        return c;
    }

    /** access_token / refresh_token (хотя бы один), необязательно expires_at; у Teams ещё client_id приложения Entra. */
    private static ObjectNode tokens(JsonNode body, ObjectNode c, boolean clientId) {
        String access = text(body, "access_token", MAX_SECRET);
        String refresh = text(body, "refresh_token", MAX_SECRET);
        if (access == null && refresh == null) throw new BadRequestException("Нужен access_token или refresh_token");
        if (access != null) c.put("access_token", access);
        if (refresh != null) c.put("refresh_token", refresh);
        JsonNode exp = body.get("expires_at");
        if (exp != null && !exp.isNull()) {
            if (!exp.isIntegralNumber() || !exp.canConvertToLong()) {
                throw new BadRequestException("expires_at: unix-время в секундах");
            }
            c.put("expires_at", exp.asLong());
        }
        String id = clientId ? text(body, "client_id", 100) : null;
        if (id != null) {
            if (!GUID.matcher(id).matches()) throw new BadRequestException("client_id: id приложения Entra (GUID)");
            c.put("client_id", id);
        }
        return c;
    }

    private PlatformResponseDTO verifyAndSave(Long userId, PlatformEnum platform, JsonNode creds) {
        CollectResult result = collector.collect(Map.of(platform, creds));
        Source s = result.source(platform);
        if (s == null) {
            // collector ничего не сказал про эту платформу — значит, он её ещё не умеет (старая версия)
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Сервис сбора ДЗ пока не умеет эту платформу: обновите collector");
        }
        if (!s.ok()) {
            String error = s == null || s.error() == null || s.error().isBlank() ? "Платформа не приняла доступ" : s.error();
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, error);
        }
        JsonNode toSave = result.updatedCredentials().getOrDefault(platform.value(), creds);
        return tx.execute(status -> {
            PlatformConnectionEntity c = connections.findByUserIdAndPlatform(userId, platform)
                    .orElseGet(() -> new PlatformConnectionEntity(userId, platform, null));
            c.connected(cipher.encrypt(toSave));
            connections.save(c);
            sync.store(userId, result.assignments());
            return PlatformResponseDTO.from(c);
        });
    }

    private void limit(Long userId) {
        if (!limiter.tryAcquire("platform:" + userId, CONNECT_LIMIT, CONNECT_WINDOW)) {
            throw new TooManyRequestsException("Слишком много попыток подключения. Попробуйте через 10 минут",
                    CONNECT_WINDOW.toSeconds());
        }
    }

    /** Строка без пробелов по краям; нет поля или пустая строка → null. */
    private static String text(JsonNode body, String field, int max) {
        JsonNode v = body.get(field);
        if (v == null || v.isNull()) return null;
        if (!v.isTextual()) throw new BadRequestException(field + ": ожидается строка");
        String s = v.asText().strip();
        if (s.isEmpty()) return null;
        if (s.length() > max) throw new BadRequestException(field + ": слишком длинное значение");
        return s;
    }

    private static String required(JsonNode body, String field, int max) {
        String s = text(body, field, max);
        if (s == null) throw new BadRequestException("Не указано поле " + field);
        return s;
    }

    /** Только https без порта и логина, домен из списка → {@code https://host} (путь и параметры отбрасываются). */
    static String allowedUrl(String raw, List<String> domains, Set<String> extraHosts) {
        return "https://" + hostOf(raw, domains, extraHosts);
    }

    static String hostOf(String raw, List<String> domains, Set<String> extraHosts) {
        URI u;
        try {
            u = new URI(raw == null ? "" : raw.strip());
        } catch (URISyntaxException e) {
            throw new BadRequestException(BAD_ADDRESS);
        }
        if (!"https".equalsIgnoreCase(u.getScheme()) || u.getRawUserInfo() != null || u.getPort() != -1
                || u.getHost() == null) {
            throw new BadRequestException(BAD_ADDRESS);
        }
        String host = u.getHost().toLowerCase(Locale.ROOT);
        boolean allowed = HOST.matcher(host).matches()
                && (extraHosts.contains(host) || domains.stream().anyMatch(d -> host.endsWith("." + d)));
        if (!allowed) throw new BadRequestException(BAD_ADDRESS);
        return host;
    }
}
