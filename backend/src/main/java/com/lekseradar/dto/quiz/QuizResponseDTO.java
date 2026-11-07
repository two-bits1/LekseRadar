package com.lekseradar.dto.quiz;

import com.lekseradar.model.enums.ExerciseLevelEnum;
import com.lekseradar.model.QuizEntity;
import com.lekseradar.model.enums.QuizStatusEnum;

import java.util.List;

/**
 * Прохождение квиза ({@code POST /quizzes}, {@code GET /quizzes/{id}}).
 *
 * @param currentPosition на каком вопросе ученик остановился (с 1)
 */
public record QuizResponseDTO(Long id, Long quizSetId, String title, String subject, String topic,
                           ExerciseLevelEnum level, QuizStatusEnum status, int currentPosition,
                           List<QuestionResponseDTO> questions) {

    /** @param subject название предмета (subjects.name) */
    public static QuizResponseDTO from(QuizEntity q, String subject, List<QuestionResponseDTO> questions) {
        return new QuizResponseDTO(q.getId(), q.getQuizSetId(), q.getTitle(), subject, q.getTopic(), q.getLevel(),
                q.getStatus(), q.getCurrentPosition() == null ? 1 : q.getCurrentPosition(), questions);
    }
}
