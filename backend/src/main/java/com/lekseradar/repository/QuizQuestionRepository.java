package com.lekseradar.repository;

import com.lekseradar.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, QuizQuestion.Key> {
    List<QuizQuestion> findByQuizIdOrderByPosition(Long quizId);
}
