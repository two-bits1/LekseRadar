package com.lekseradar.dto.chat;

import com.lekseradar.model.ChatMessageEntity;
import com.lekseradar.model.enums.MessageRole;

import java.time.Instant;

/**
 * @param role    "student" или "tutor"
 * @param content у tutor — текст в Markdown
 * @param model   модель, которая ответила (только у tutor)
 */
public record MessageResponse(Long id, MessageRole role, String content, String model, Instant createdAt) {

    public static MessageResponse from(ChatMessageEntity m) {
        return new MessageResponse(m.getId(), m.getRole(), m.getContent(), m.getModel(), m.getCreatedAt());
    }
}
