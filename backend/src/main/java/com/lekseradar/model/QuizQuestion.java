package com.lekseradar.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.Objects;

/** Таблица {@code quiz_questions} (V4): какие вопросы в каком порядке в прохождении квиза. */
@Entity
@Immutable
@Table(name = "quiz_questions")
@IdClass(QuizQuestion.Key.class)
public class QuizQuestion {
    @Id
    @Column(name = "quiz_id")
    private Long quizId;

    @Id
    private Integer position;

    @Column(name = "exercise_id")
    private Long exerciseId;

    protected QuizQuestion() {}

    public Long getQuizId() { return quizId; }
    public Integer getPosition() { return position; }
    public Long getExerciseId() { return exerciseId; }

    public static class Key implements Serializable {
        private Long quizId;
        private Integer position;

        public Key() {}

        public Key(Long quizId, Integer position) {
            this.quizId = quizId;
            this.position = position;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(quizId, k.quizId) && Objects.equals(position, k.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(quizId, position);
        }
    }
}
