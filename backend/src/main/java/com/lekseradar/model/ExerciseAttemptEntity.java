package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Таблица {@code exercise_attempts} (V4): ответ пользователя на вопрос. Пишет save_quiz_answer;
 * неверный или частичный ответ сам попадает в колоду (триггер attempt_to_flashcard).
 */
@Entity
@Immutable
@Table(name = "exercise_attempts")
public class ExerciseAttemptEntity {
    @Id
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(name = "quiz_id")
    private Long quizId;

    private String answer;

    @Column(columnDefinition = "verdict")
    private Verdict verdict;

    private String explanation;

    @Column(name = "created_at")
    private Instant createdAt;

    protected ExerciseAttemptEntity() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getExerciseId() { return exerciseId; }
    public Long getQuizId() { return quizId; }
    public String getAnswer() { return answer; }
    public Verdict getVerdict() { return verdict; }
    public String getExplanation() { return explanation; }
    public Instant getCreatedAt() { return createdAt; }
}
