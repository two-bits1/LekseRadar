package com.lekseradar.dto.quiz;

import com.lekseradar.model.enums.ExerciseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /quizzes}: найти готовый набор или сгенерировать, затем начать.
 *
 * @param level        по умолчанию intermediate
 * @param assignmentId квиз по конкретному ДЗ или null
 */
public record StartQuizRequest(
        @NotBlank @Size(max = 100) String subject,
        @NotBlank @Size(max = 200) String topic,
        ExerciseLevel level,
        Long assignmentId) {

    public ExerciseLevel levelOrDefault() {
        return level == null ? ExerciseLevel.INTERMEDIATE : level;
    }
}
