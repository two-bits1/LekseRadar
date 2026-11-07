package com.lekseradar.dto.homework;

import com.lekseradar.model.enums.PlatformEnum;

/**
 * Результат сбора ДЗ с одной платформы.
 *
 * @param error          текст ошибки или null
 * @param needsReconnect true — нужно заново войти в платформу
 */
public record SourceStatusResponseDTO(PlatformEnum platform, boolean ok, int count, String error, boolean needsReconnect) {}
