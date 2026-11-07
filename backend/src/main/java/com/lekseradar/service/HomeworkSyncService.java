package com.lekseradar.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.dto.homework.SourceStatusResponseDTO;
import com.lekseradar.dto.homework.SyncResponseDTO;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.model.PlatformConnectionEntity;
import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.repository.AssignmentRepository;
import com.lekseradar.repository.PlatformConnectionRepository;
import com.lekseradar.repository.UserEventRepository;
import com.lekseradar.security.CredentialsCipher;
import com.lekseradar.security.RateLimiter;
import com.lekseradar.service.collector.CollectorClient;
import com.lekseradar.service.collector.CollectorClient.CollectResult;
import com.lekseradar.service.collector.CollectorClient.Source;
import com.lekseradar.service.google.GoogleOAuthClient;
import com.lekseradar.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * {@code POST /homework/sync}: расшифровать все подключения ученика → один {@code collector /collect} →
 * {@code sync_assignments()} → статус каждой платформы. Вызов collector — до транзакции, чтобы не держать
 * соединение с базой, пока он ходит по платформам.
 */
@Service
public class HomeworkSyncService {
    static final Duration SYNC_WINDOW = Duration.ofMinutes(1);
    static final String RECONNECT = "Подключите платформу заново";

    private final PlatformConnectionRepository connections;
    private final AssignmentRepository assignments;
    private final UserEventRepository events;
    private final CollectorClient collector;
    private final CredentialsCipher cipher;
    private final RateLimiter limiter;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    private final GoogleOAuthClient google;

    public HomeworkSyncService(PlatformConnectionRepository connections, AssignmentRepository assignments,
                               UserEventRepository events, CollectorClient collector, CredentialsCipher cipher,
                               RateLimiter limiter, ObjectMapper json, PlatformTransactionManager txManager,
                               GoogleOAuthClient google) {
        this.google = google;
        this.connections = connections;
        this.assignments = assignments;
        this.events = events;
        this.collector = collector;
        this.cipher = cipher;
        this.limiter = limiter;
        this.json = json;
        this.tx = new TransactionTemplate(txManager);
    }

    public record Counts(int added, int updated) {}

    /** Кнопка «Oppdater»: не чаще раза в минуту. */
    public SyncResponseDTO sync(Long userId) {
        if (!limiter.tryAcquire("sync:" + userId, 1, SYNC_WINDOW)) {
            throw new TooManyRequestsException("ДЗ только что обновлялись. Попробуйте через минуту", SYNC_WINDOW.toSeconds());
        }
        return syncNow(userId);
    }

    /** Без лимита: для расписания. */
    public SyncResponseDTO syncNow(Long userId) {
        List<SourceStatusResponseDTO> sources = new ArrayList<>();
        Map<PlatformEnum, JsonNode> creds = new EnumMap<>(PlatformEnum.class);
        Map<PlatformEnum, Long> ids = new EnumMap<>(PlatformEnum.class);

        Map<PlatformEnum, JsonNode> refreshed = new EnumMap<>(PlatformEnum.class);

        for (PlatformConnectionEntity c : connections.findByUserIdOrderByPlatform(userId)) {
            if (c.isNeedsReconnect()) {
                sources.add(reconnect(c.getPlatform(), c.getLastError()));
                continue;
            }
            JsonNode decrypted;
            try {
                decrypted = cipher.decrypt(c.getCredentialsEnc());
            } catch (IllegalStateException e) {
                // Сменился CREDENTIALS_KEY или байты испорчены: доступ не восстановить, только подключить заново
                c.markSynced(false, RECONNECT, true);
                connections.save(c);
                sources.add(reconnect(c.getPlatform(), RECONNECT));
                continue;
            }
            if (c.getPlatform() == PlatformEnum.CLASSROOM) {
                // Ключ Google живёт час: бэкенд сам обновляет его по refresh_token, collector получает готовый
                try {
                    JsonNode fresh = google.fresh(decrypted);
                    if (fresh != decrypted) refreshed.put(c.getPlatform(), fresh);
                    decrypted = fresh;
                } catch (ApiException e) {
                    boolean reconnect = e.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY;
                    c.markSynced(false, e.getMessage(), reconnect);
                    connections.save(c);
                    sources.add(new SourceStatusResponseDTO(c.getPlatform(), false, 0, e.getMessage(), reconnect));
                    continue;
                }
            }
            creds.put(c.getPlatform(), decrypted);
            ids.put(c.getPlatform(), c.getId());
        }
        if (creds.isEmpty()) {
            return new SyncResponseDTO(sorted(sources), 0, 0);
        }

        CollectResult result = collector.collect(creds);

        return tx.execute(status -> {
            ids.forEach((platform, id) -> connections.findById(id).ifPresent(c -> {   // ученик мог отключить её за это время
                Source s = result.source(platform);
                if (s == null) s = new Source(false, 0, "Платформа не ответила", false);
                c.markSynced(s.ok(), s.error(), s.needsReconnect());
                JsonNode updated = result.updatedCredentials().getOrDefault(platform.value(), refreshed.get(platform));
                if (updated != null) c.setCredentialsEnc(cipher.encrypt(updated));
                connections.save(c);
                sources.add(new SourceStatusResponseDTO(platform, s.ok(), s.count(), s.error(), c.isNeedsReconnect()));
            }));
            Counts n = store(userId, result.assignments());
            return new SyncResponseDTO(sorted(sources), n.added(), n.updated());
        });
    }

    /** Задания в базу ({@code sync_assignments}, V21) и событие {@code homework_synced}. Вызывать в транзакции. */
    public Counts store(Long userId, JsonNode items) {
        AssignmentRepository.SyncCounts c;
        try {
            c = assignments.syncAssignments(userId, json.writeValueAsString(items));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        Counts n = new Counts(c == null || c.getAdded() == null ? 0 : c.getAdded(),
                c == null || c.getUpdated() == null ? 0 : c.getUpdated());
        events.addWithPayload(userId, "homework_synced",
                json.createObjectNode().put("added", n.added()).put("updated", n.updated()).toString());
        return n;
    }

    private static SourceStatusResponseDTO reconnect(PlatformEnum p, String error) {
        return new SourceStatusResponseDTO(p, false, 0, error == null ? RECONNECT : error, true);
    }

    private static List<SourceStatusResponseDTO> sorted(List<SourceStatusResponseDTO> list) {
        list.sort(Comparator.comparing(SourceStatusResponseDTO::platform));
        return list;
    }
}
