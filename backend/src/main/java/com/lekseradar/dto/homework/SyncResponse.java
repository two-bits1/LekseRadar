package com.lekseradar.dto.homework;

import java.util.List;

/**
 * Ответ {@code POST /homework/sync}.
 *
 * @param count сколько ДЗ сохранено всего
 */
public record SyncResponse(List<SourceStatus> sources, int count) {}
