package com.lekseradar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Раз в 15 минут шлёт напоминания о ДЗ со сроком в ближайшие 24 часа. Выключается {@code REMINDERS_ENABLED=false}. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.reminders.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private final ReminderService reminders;

    public ReminderScheduler(ReminderService reminders) {
        this.reminders = reminders;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT15M")
    public void run() {
        try {
            reminders.remindDue();
        } catch (RuntimeException e) {
            log.warn("email reminders failed", e);
        }
    }
}
