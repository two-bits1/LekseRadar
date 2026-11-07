package com.lekseradar.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ChatDtos {
    private ChatDtos() {}

    public record CreateSessionRequest(@Size(max = 200) String title, @Size(max = 100) String subject) {}

    public record UpdateSessionRequest(@Size(max = 200) String title, @Size(max = 100) String subject) {}

    public record SendMessageRequest(@NotBlank @Size(max = 8000) String content) {}

    public record SessionDto(Long id, String title, String subject, Instant createdAt, Instant updatedAt) {
        static SessionDto from(ChatSession s) {
            return new SessionDto(s.getId(), s.getTitle(), s.getSubject(), s.getCreatedAt(), s.getUpdatedAt());
        }
    }

    public record MessageDto(Long id, String role, String content, Instant createdAt) {
        static MessageDto from(ChatMessage m) {
            return new MessageDto(m.getId(), m.getRole().name(), m.getContent(), m.getCreatedAt());
        }
    }

    /** Ответ на отправку сообщения: сохранённое сообщение пользователя + ответ ИИ. */
    public record ExchangeDto(SessionDto session, MessageDto userMessage, MessageDto assistantMessage) {}
}
