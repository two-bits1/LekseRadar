package com.lekseradar.repository;

import com.lekseradar.model.ExerciseAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttemptEntity, Long> {
    List<ExerciseAttemptEntity> findByQuizIdOrderByCreatedAt(Long quizId);

    List<ExerciseAttemptEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
