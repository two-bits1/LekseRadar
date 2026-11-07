package com.lekseradar.repository;

import com.lekseradar.model.QuizSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuizSetRepository extends JpaRepository<QuizSet, Long> {

    List<QuizSet> findBySubjectIdOrderByCreatedAtDesc(Long subjectId);

    /** Готовый квиз по предмету, теме, уровню и языку (регистр и пробелы не важны) или null. */
    @Query(value = "SELECT find_quiz_set(:subject, :topic, CAST(:level AS exercise_level), :language)",
            nativeQuery = true)
    Long find(@Param("subject") String subject, @Param("topic") String topic,
              @Param("level") String level, @Param("language") String language);

    /**
     * Сохранить JSON квиза от ИИ (database/quiz/quiz.schema.json): id набора. Тот же JSON второй раз вернёт
     * тот же id. Неверный JSON → ошибка базы с текстом «quiz json: …».
     */
    @Transactional
    @Query(value = "SELECT import_quiz_json(CAST(:json AS jsonb), :userId)", nativeQuery = true)
    Long importJson(@Param("json") String json, @Param("userId") Long userId);
}
