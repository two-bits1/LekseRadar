package com.lekseradar.dto.quiz;

import com.lekseradar.model.enums.ExerciseLevelEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /quizzes} и {@code POST /flashcards/generate}: найти готовый набор вопросов или попросить ИИ составить.
 *
 * @param level        по умолчанию intermediate
 * @param assignmentId квиз по конкретному ДЗ (ИИ получит текст задания) или null
 * @param count        сколько вопросов просить у ИИ: 1–30, по умолчанию 8
 * @param fresh        true — всегда новый квиз от ИИ, даже если готовый уже есть в базе
 */
public record StartQuizRequestDTO(
        @NotBlank @Size(max = 100) String subject,
        @NotBlank @Size(max = 100) String topic,
        ExerciseLevelEnum level,
        Long assignmentId,
        @Min(1) @Max(30) Integer count,
        Boolean fresh) {

    public ExerciseLevelEnum levelOrDefault() {
        return level == null ? ExerciseLevelEnum.INTERMEDIATE : level;
    }
}
