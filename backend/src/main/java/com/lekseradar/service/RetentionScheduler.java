package com.lekseradar.service;

import com.lekseradar.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Срок хранения (GDPR, ст. 5(1)(e)): раз в сутки удаляет аккаунты, в которые не входили
 * {@code RETENTION_INACTIVE_MONTHS} месяцев (по умолчанию 24; 0 — выключено), со всеми данными.
 * Этот срок назван в политике конфиденциальности.
 */
@Configuration
@EnableScheduling
public class RetentionScheduler {
    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final UserRepository users;
    private final AccountService accounts;
    private final int months;

    public RetentionScheduler(UserRepository users, AccountService accounts,
                              @Value("${app.retention.inactive-months:24}") int months) {
        this.users = users;
        this.accounts = accounts;
        this.months = months;
    }

    @Scheduled(cron = "0 17 3 * * *", zone = "Europe/Oslo")
    public void deleteInactive() {
        if (months <= 0) return;
        Instant before = Instant.now().atZone(ZoneOffset.UTC).minusMonths(months).toInstant();
        List<Long> ids = users.findInactiveSince(before);
        for (Long id : ids) {
            try {
                accounts.deleteAll(id);
            } catch (RuntimeException e) {
                log.warn("retention: could not delete account {}", id, e);
            }
        }
        if (!ids.isEmpty()) log.info("retention: deleted {} accounts inactive for {} months", ids.size(), months);
    }
}
