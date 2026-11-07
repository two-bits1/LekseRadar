package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Таблица {@code quiz_sets} (V7): квиз от ИИ, сохранённый один раз и переиспользуемый.
 * Пишет только функция import_quiz_json (проверяет JSON), поэтому сущность только для чтения.
 */
@Entity
@Immutable
@Table(name = "quiz_sets")
public class QuizSetEntity {
    @Id
    private Long id;

    @Column(name = "subject_id")
    private Long subjectId;

    private String topic;

    @Column(name = "topic_key")
    private String topicKey;

    @Column(columnDefinition = "exercise_level")
    private ExerciseLevel level;

    private String language;

    private String title;

    @Column(name = "question_count")
    private Integer questionCount;

    /** Исходный JSON от ИИ (формат database/quiz/quiz.schema.json). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_json")
    private JsonNode sourceJson;

    @Column(name = "content_hash")
    private String contentHash;

    private String model;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "times_used")
    private Integer timesUsed;

    /** Материалы, по которым ИИ делал квиз. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "material_ids", columnDefinition = "bigint[]")
    private Long[] materialIds;

    @Column(name = "prompt_id")
    private Long promptId;

    @Column(name = "created_at")
    private Instant createdAt;

    protected QuizSetEntity() {}

    public Long getId() { return id; }
    public Long getSubjectId() { return subjectId; }
    public String getTopic() { return topic; }
    public String getTopicKey() { return topicKey; }
    public ExerciseLevel getLevel() { return level; }
    public String getLanguage() { return language; }
    public String getTitle() { return title; }
    public Integer getQuestionCount() { return questionCount; }
    public JsonNode getSourceJson() { return sourceJson; }
    public String getContentHash() { return contentHash; }
    public String getModel() { return model; }
    public Long getCreatedBy() { return createdBy; }
    public Integer getTimesUsed() { return timesUsed; }
    public Long[] getMaterialIds() { return materialIds; }
    public Long getPromptId() { return promptId; }
    public Instant getCreatedAt() { return createdAt; }
}
