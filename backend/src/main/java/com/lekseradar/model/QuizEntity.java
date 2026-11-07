package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Таблица {@code quizzes} (V4, V7, V8): одно прохождение квиза пользователем.
 * Создаёт start_quiz, ответы пишет save_quiz_answer, балл ставит finish_quiz.
 */
@Entity
@Immutable
@Table(name = "quizzes")
public class QuizEntity {
    @Id
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "chat_session_id")
    private Long chatSessionId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "quiz_set_id")
    private Long quizSetId;

    private String title;

    private String topic;

    @Column(columnDefinition = "exercise_level")
    private ExerciseLevel level;

    @Column(columnDefinition = "quiz_status")
    private QuizStatus status;

    /** 0–100, после finish_quiz. */
    private BigDecimal score;

    /** Где пользователь остановился (номер вопроса). */
    @Column(name = "current_position")
    private Integer currentPosition;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected QuizEntity() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getAssignmentId() { return assignmentId; }
    public Long getChatSessionId() { return chatSessionId; }
    public Long getSubjectId() { return subjectId; }
    public Long getQuizSetId() { return quizSetId; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public ExerciseLevel getLevel() { return level; }
    public QuizStatus getStatus() { return status; }
    public BigDecimal getScore() { return score; }
    public Integer getCurrentPosition() { return currentPosition; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
