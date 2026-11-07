package com.lekseradar.repository;

import com.lekseradar.model.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, Long> {
    List<ExerciseAttempt> findByQuizIdOrderByCreatedAt(Long quizId);

    List<ExerciseAttempt> findByUserIdOrderByCreatedAtDesc(Long userId);
}
