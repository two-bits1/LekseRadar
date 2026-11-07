package com.lekseradar.repository;

import com.lekseradar.model.QuizQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestionEntity, QuizQuestionEntity.Key> {
    List<QuizQuestionEntity> findByQuizIdOrderByPosition(Long quizId);
}
