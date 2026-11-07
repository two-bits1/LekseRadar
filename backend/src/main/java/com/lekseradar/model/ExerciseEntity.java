package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Таблица {@code exercises} (V4, V7): вопрос квиза; он же флеш-карта.
 * Создаёт import_quiz_json. {@code correctAnswer} не отдавать во фронт до ответа:
 * для экрана квиза есть вид quiz_questions_public.
 */
@Entity
@Immutable
@Table(name = "exercises")
public class ExerciseEntity {
    @Id
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "quiz_set_id")
    private Long quizSetId;

    /** Номер в наборе, с 1. */
    private Integer position;

    private String topic;

    @Column(columnDefinition = "exercise_level")
    private ExerciseLevelEnum level;

    @Column(name = "question_type")
    private QuestionTypeEnum questionType;

    private String statement;

    /** Варианты ответа (JSON-массив) или null для text. */
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode options;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "answer_type", columnDefinition = "answer_type")
    private AnswerTypeEnum answerType;

    private String explanation;

    private boolean generated;

    @Column(name = "created_at")
    private Instant createdAt;

    protected ExerciseEntity() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getAssignmentId() { return assignmentId; }
    public Long getSubjectId() { return subjectId; }
    public Long getQuizSetId() { return quizSetId; }
    public Integer getPosition() { return position; }
    public String getTopic() { return topic; }
    public ExerciseLevelEnum getLevel() { return level; }
    public QuestionTypeEnum getQuestionType() { return questionType; }
    public String getStatement() { return statement; }
    public JsonNode getOptions() { return options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public AnswerTypeEnum getAnswerType() { return answerType; }
    public String getExplanation() { return explanation; }
    public boolean isGenerated() { return generated; }
    public Instant getCreatedAt() { return createdAt; }
}
