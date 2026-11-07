package com.lekseradar.dto.quiz;

import com.lekseradar.model.ExerciseLevel;
import com.lekseradar.model.Quiz;
import com.lekseradar.model.QuizStatus;

import java.util.List;

/**
 * Прохождение квиза ({@code POST /quizzes}, {@code GET /quizzes/{id}}).
 *
 * @param currentPosition на каком вопросе ученик остановился (с 1)
 */
public record QuizResponse(Long id, Long quizSetId, String title, String subject, String topic,
                           ExerciseLevel level, QuizStatus status, int currentPosition,
                           List<QuestionResponse> questions) {

    /** @param subject название предмета (subjects.name) */
    public static QuizResponse from(Quiz q, String subject, List<QuestionResponse> questions) {
        return new QuizResponse(q.getId(), q.getQuizSetId(), q.getTitle(), subject, q.getTopic(), q.getLevel(),
                q.getStatus(), q.getCurrentPosition() == null ? 1 : q.getCurrentPosition(), questions);
    }
}
