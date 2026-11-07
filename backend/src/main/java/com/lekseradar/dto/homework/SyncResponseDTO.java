package com.lekseradar.dto.homework;

import java.util.List;

/**
 * Ответ {@code POST /homework/sync}.
 *
 * @param count сколько ДЗ сохранено всего
 */
public record SyncResponseDTO(List<SourceStatusResponseDTO> sources, int count) {}
