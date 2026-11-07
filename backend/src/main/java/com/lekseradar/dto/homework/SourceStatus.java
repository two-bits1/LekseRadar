package com.lekseradar.dto.homework;

import com.lekseradar.model.Platform;

/**
 * Результат сбора ДЗ с одной платформы.
 *
 * @param error          текст ошибки или null
 * @param needsReconnect true — нужно заново войти в платформу
 */
public record SourceStatus(Platform platform, boolean ok, int count, String error, boolean needsReconnect) {}
