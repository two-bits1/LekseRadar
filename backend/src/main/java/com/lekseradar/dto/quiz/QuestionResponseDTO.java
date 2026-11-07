package com.lekseradar.dto.quiz;

import com.lekseradar.model.enums.DbEnum;
import com.lekseradar.model.enums.QuestionTypeEnum;
import com.lekseradar.repository.QuizRepository;

import java.util.List;

/**
 * Вопрос квиза для экрана. Правильного ответа здесь нет: данные берутся только из вида quiz_questions_public.
 *
 * @param options варианты для single/multiple_choice, null для text
 */
public record QuestionResponseDTO(int position, Long exerciseId, QuestionTypeEnum type, String question,
                               List<String> options) {

    /** @param options варианты, уже разобранные из JSON-строки {@code PublicQuestion.getOptions()} */
    public static QuestionResponseDTO from(QuizRepository.PublicQuestion q, List<String> options) {
        return new QuestionResponseDTO(q.getPosition(), q.getExerciseId(),
                DbEnum.fromValue(QuestionTypeEnum.class, q.getQuestionType()), q.getQuestion(), options);
    }
}
