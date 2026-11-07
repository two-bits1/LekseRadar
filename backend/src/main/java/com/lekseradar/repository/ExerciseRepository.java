package com.lekseradar.repository;

import com.lekseradar.model.ExerciseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<ExerciseEntity, Long> {
    /** Вопросы набора по порядку (с правильными ответами — только для бэкенда и карточек). */
    List<ExerciseEntity> findByQuizSetIdOrderByPosition(Long quizSetId);
}
