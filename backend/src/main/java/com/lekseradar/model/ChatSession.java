package com.lekseradar.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Таблица {@code chat_sessions} (database/migrations/V3__chat.sql).
 * Колонка assignment_id (чат по ДЗ) пока не используется.
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSession {
    /** Значение по умолчанию в базе; такой чат получает название по первому вопросу. */
    public static final String DEFAULT_TITLE = "Ny samtale";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    private String topic;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Триггер в базе тоже обновляет это поле при каждом новом сообщении. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ChatSession() {}

    public ChatSession(Long userId, String title, String topic) {
        this.userId = userId;
        this.title = title;
        this.topic = topic;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void touch() { this.updatedAt = Instant.now(); }
}
