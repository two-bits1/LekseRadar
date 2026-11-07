package com.lekseradar.dto.quiz;

import com.lekseradar.model.QuizEntity;
import com.lekseradar.model.enums.ExerciseLevelEnum;
import com.lekseradar.model.enums.QuizStatusEnum;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Квиз в списке {@code GET /quizzes} (без вопросов).
 *
 * @param chatId квиз-чат, где ИИ составил вопросы (null — набор взят готовый из базы)
 * @param score  0–100 после завершения, иначе null
 */
public record QuizSummaryResponseDTO(Long id, Long quizSetId, Long chatId, Long assignmentId, String title,
                                     String subject, String topic, ExerciseLevelEnum level, QuizStatusEnum status,
                                     BigDecimal score, int currentPosition, int questionCount,
                                     Instant startedAt, Instant completedAt) {

    public static QuizSummaryResponseDTO from(QuizEntity q, String subject, int questionCount) {
        return new QuizSummaryResponseDTO(q.getId(), q.getQuizSetId(), q.getChatSessionId(), q.getAssignmentId(),
                q.getTitle(), subject, q.getTopic(), q.getLevel(), q.getStatus(), q.getScore(),
                q.getCurrentPosition() == null ? 1 : q.getCurrentPosition(), questionCount,
                q.getStartedAt(), q.getCompletedAt());
    }
}
