package com.lekseradar.service;

import com.lekseradar.exception.ApiException;
import com.lekseradar.repository.PlatformConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Раз в 30 минут обновляет ДЗ ученикам, которые заходили за последние 7 дней и у которых есть подключения.
 * Ошибка одного ученика не останавливает остальных. Выключается {@code SYNC_SCHEDULE_ENABLED=false}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.sync.schedule-enabled", havingValue = "true", matchIfMissing = true)
public class HomeworkSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(HomeworkSyncScheduler.class);
    static final Duration ACTIVE = Duration.ofDays(7);

    private final PlatformConnectionRepository connections;
    private final HomeworkSyncService sync;

    public HomeworkSyncScheduler(PlatformConnectionRepository connections, HomeworkSyncService sync) {
        this.connections = connections;
        this.sync = sync;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT30M")
    public void syncActiveStudents() {
        List<Long> users = connections.findUserIdsToSync(Instant.now().minus(ACTIVE));
        int ok = 0;
        for (Long userId : users) {
            try {
                sync.syncNow(userId);
                ok++;
            } catch (ApiException e) {
                log.warn("scheduled sync failed for user {}: {}", userId, e.getMessage());
                if (e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) break;   // collector лежит: остальных не мучаем
            } catch (RuntimeException e) {
                log.warn("scheduled sync failed for user {}", userId, e);
            }
        }
        if (!users.isEmpty()) log.info("scheduled sync: {} of {} students", ok, users.size());
    }
}
