package com.lekseradar.repository;

import com.lekseradar.model.QuizSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuizSetRepository extends JpaRepository<QuizSetEntity, Long> {

    List<QuizSetEntity> findBySubjectIdOrderByCreatedAtDesc(Long subjectId);

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

    /** Каким промптом составлен набор (quiz_sets.prompt_id, V12): пишется один раз, при первом импорте. */
    @Modifying
    @Transactional
    @Query(value = "UPDATE quiz_sets SET prompt_id = :promptId WHERE id = :setId AND prompt_id IS NULL",
            nativeQuery = true)
    int setPromptIfEmpty(@Param("setId") Long setId, @Param("promptId") Long promptId);
}
