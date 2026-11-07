package com.lekseradar.exercise;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "exercise_attempts")
public class ExerciseAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ExerciseAttempt() {}

    public ExerciseAttempt(Long exerciseId, Long userId, String answer, boolean correct, int score, String feedback) {
        this.exerciseId = exerciseId;
        this.userId = userId;
        this.answer = answer;
        this.correct = correct;
        this.score = score;
        this.feedback = feedback;
    }

    public Long getId() { return id; }
    public Long getExerciseId() { return exerciseId; }
    public Long getUserId() { return userId; }
    public String getAnswer() { return answer; }
    public boolean isCorrect() { return correct; }
    public int getScore() { return score; }
    public String getFeedback() { return feedback; }
    public Instant getCreatedAt() { return createdAt; }
}
