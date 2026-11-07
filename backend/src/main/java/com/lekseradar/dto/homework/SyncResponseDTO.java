package com.lekseradar.dto.homework;

import java.util.List;

/**
 * Ответ {@code POST /homework/sync}.
 *
 * @param sources по одной строке на подключённую платформу
 * @param added   новых заданий
 * @param updated уже известных заданий обновлено
 * @param count   added + updated
 */
public record SyncResponseDTO(List<SourceStatusResponseDTO> sources, int added, int updated, int count) {

    public SyncResponseDTO(List<SourceStatusResponseDTO> sources, int added, int updated) {
        this(sources, added, updated, added + updated);
    }
}
