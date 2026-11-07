package com.lekseradar.dto.chat;

import com.lekseradar.model.ChatMessageEntity;
import com.lekseradar.model.enums.MessageRoleEnum;

import java.time.Instant;

/**
 * @param role    "student" или "tutor"
 * @param content у tutor — текст в Markdown
 * @param model   модель, которая ответила (только у tutor)
 */
public record MessageResponseDTO(Long id, MessageRoleEnum role, String content, String model, Instant createdAt) {

    public static MessageResponseDTO from(ChatMessageEntity m) {
        return new MessageResponseDTO(m.getId(), m.getRole(), m.getContent(), m.getModel(), m.getCreatedAt());
    }
}
