package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

/**
 * Таблица {@code chat_sessions} (V3, V13, V14). Режим {@link ChatModeEnum} задаётся при создании и не меняется
 * (триггер chat_mode_locked). Чат с предметом и темой удобнее создавать функцией start_chat
 * ({@link com.lekseradar.repository.ChatSessionRepository#startChat}).
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSessionEntity {
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

    /** Чат по конкретному ДЗ (assignments.id) или null. */
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ColumnTransformer(write = "?::chat_mode")
    @Column(nullable = false, updatable = false, columnDefinition = "chat_mode")
    private ChatModeEnum mode = ChatModeEnum.TUTOR;

    @Column(name = "subject_id")
    private Long subjectId;

    /** Только у чата quiz: какой набор вопросов он создал. */
    @Column(name = "quiz_set_id")
    private Long quizSetId;

    /** Язык ответа ИИ; null — тьютор сначала спрашивает язык. */
    private String language;

    @ColumnTransformer(write = "?::exercise_level")
    @Column(nullable = false, columnDefinition = "exercise_level")
    private ExerciseLevelEnum level = ExerciseLevelEnum.INTERMEDIATE;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Триггер в базе тоже обновляет это поле при каждом новом сообщении. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ChatSessionEntity() {}

    public ChatSessionEntity(Long userId, String title, String topic) {
        this.userId = userId;
        this.title = title;
        this.topic = topic;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public Long getAssignmentId() { return assignmentId; }
    public ChatModeEnum getMode() { return mode; }
    public Long getSubjectId() { return subjectId; }
    public Long getQuizSetId() { return quizSetId; }
    public String getLanguage() { return language; }
    public ExerciseLevelEnum getLevel() { return level; }
    public boolean isArchived() { return archived; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void setQuizSetId(Long quizSetId) { this.quizSetId = quizSetId; }
    public void setLevel(ExerciseLevelEnum level) { this.level = level; }
    public void touch() { this.updatedAt = Instant.now(); }
}
