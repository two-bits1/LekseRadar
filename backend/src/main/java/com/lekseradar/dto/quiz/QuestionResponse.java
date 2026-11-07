package com.lekseradar.dto.quiz;

import com.lekseradar.model.DbEnum;
import com.lekseradar.model.QuestionType;
import com.lekseradar.repository.QuizRepository;

import java.util.List;

/**
 * Вопрос квиза для экрана. Правильного ответа здесь нет: данные берутся только из вида quiz_questions_public.
 *
 * @param options варианты для single/multiple_choice, null для text
 */
public record QuestionResponse(int position, Long exerciseId, QuestionType type, String question,
                               List<String> options) {

    /** @param options варианты, уже разобранные из JSON-строки {@code PublicQuestion.getOptions()} */
    public static QuestionResponse from(QuizRepository.PublicQuestion q, List<String> options) {
        return new QuestionResponse(q.getPosition(), q.getExerciseId(),
                DbEnum.fromValue(QuestionType.class, q.getQuestionType()), q.getQuestion(), options);
    }
}
