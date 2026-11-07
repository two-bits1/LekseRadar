package com.lekseradar.dto.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.lekseradar.model.UserEventEntity;

import java.time.Instant;

/**
 * Запись ленты «что я делал» ({@code GET /history}).
 *
 * @param type login | chat_started | quiz_completed | homework_synced | …
 */
public record EventResponse(Long id, String type, String refTable, Long refId, JsonNode payload, Instant createdAt) {

    public static EventResponse from(UserEventEntity e) {
        return new EventResponse(e.getId(), e.getType(), e.getRefTable(), e.getRefId(), e.getPayload(),
                e.getCreatedAt());
    }
}
