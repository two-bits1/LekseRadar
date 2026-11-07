package com.lekseradar.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Таблица {@code flashcard_progress} (V9): карточка в колоде пользователя = вопрос квиза + прогресс SM-2.
 * Пишут add_quiz_set_to_flashcards, review_flashcard и триггер attempt_to_flashcard.
 */
@Entity
@Immutable
@Table(name = "flashcard_progress")
@IdClass(FlashcardProgress.Key.class)
public class FlashcardProgress {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "exercise_id")
    private Long exerciseId;

    private BigDecimal ease;

    @Column(name = "interval_days")
    private Integer intervalDays;

    private Integer repetitions;

    private Integer reviews;

    private Integer lapses;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "added_at")
    private Instant addedAt;

    protected FlashcardProgress() {}

    public Long getUserId() { return userId; }
    public Long getExerciseId() { return exerciseId; }
    public BigDecimal getEase() { return ease; }
    public Integer getIntervalDays() { return intervalDays; }
    public Integer getRepetitions() { return repetitions; }
    public Integer getReviews() { return reviews; }
    public Integer getLapses() { return lapses; }
    public Instant getNextReviewAt() { return nextReviewAt; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public Instant getAddedAt() { return addedAt; }

    public static class Key implements Serializable {
        private Long userId;
        private Long exerciseId;

        public Key() {}

        public Key(Long userId, Long exerciseId) {
            this.userId = userId;
            this.exerciseId = exerciseId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(userId, k.userId) && Objects.equals(exerciseId, k.exerciseId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, exerciseId);
        }
    }
}
