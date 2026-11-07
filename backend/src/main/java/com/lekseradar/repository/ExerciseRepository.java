package com.lekseradar.repository;

import com.lekseradar.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    /** Вопросы набора по порядку (с правильными ответами — только для бэкенда и карточек). */
    List<Exercise> findByQuizSetIdOrderByPosition(Long quizSetId);
}
