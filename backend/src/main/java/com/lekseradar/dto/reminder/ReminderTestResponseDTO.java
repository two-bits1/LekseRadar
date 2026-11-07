package com.lekseradar.dto.reminder;

/**
 * Пробное напоминание отправлено.
 *
 * @param email куда ушло письмо (почта из профиля)
 * @param count сколько заданий в письме: несданные со сроком в ближайшие 7 дней и просроченные
 */
public record ReminderTestResponseDTO(String email, int count) {}
