package com.lekseradar.model;

import com.lekseradar.model.converter.MessageRoleConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

/** Таблица {@code chat_messages} (database/migrations/V3__chat.sql). */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    /** CHECK в базе: length(content) BETWEEN 1 AND 8000. */
    public static final int MAX_CONTENT_LENGTH = 8000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    @Convert(converter = MessageRoleConverter.class)
    @ColumnTransformer(write = "?::chat_role")
    @Column(nullable = false)
    private MessageRole role;

    @Column(nullable = false)
    private String content;

    /** Только у ответов тьютора: относится ли вопрос к учёбе. */
    @Column(name = "on_topic")
    private Boolean onTopic;

    /** Какая модель ответила. */
    private String model;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ChatMessage() {}

    public static ChatMessage fromStudent(Long sessionId, String content) {
        ChatMessage m = new ChatMessage();
        m.sessionId = sessionId;
        m.role = MessageRole.STUDENT;
        m.content = content;
        return m;
    }

    public static ChatMessage fromTutor(Long sessionId, String content, String model, Integer tokensUsed) {
        ChatMessage m = new ChatMessage();
        m.sessionId = sessionId;
        m.role = MessageRole.TUTOR;
        m.content = content;
        m.model = model;
        m.tokensUsed = tokensUsed;
        return m;
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public MessageRole getRole() { return role; }
    public String getContent() { return content; }
    public Boolean getOnTopic() { return onTopic; }
    public String getModel() { return model; }
    public Integer getTokensUsed() { return tokensUsed; }
    public Instant getCreatedAt() { return createdAt; }
}
