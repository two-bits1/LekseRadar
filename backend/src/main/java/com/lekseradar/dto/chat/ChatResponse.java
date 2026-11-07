package com.lekseradar.dto.chat;

import com.lekseradar.model.ChatSessionEntity;

import java.time.Instant;

public record ChatResponse(Long id, String title, String topic, boolean archived, Instant createdAt,
                           Instant updatedAt) {

    public static ChatResponse from(ChatSessionEntity s) {
        return new ChatResponse(s.getId(), s.getTitle(), s.getTopic(), s.isArchived(), s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
