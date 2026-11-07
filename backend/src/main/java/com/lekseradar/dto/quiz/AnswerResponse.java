package com.lekseradar.dto.quiz;

import com.lekseradar.model.enums.DbEnum;
import com.lekseradar.model.enums.Verdict;
import com.lekseradar.repository.QuizRepository;

/** Результат ответа: вердикт, пояснение и правильный ответ (показывается после ответа). */
public record AnswerResponse(Verdict verdict, String explanation, String correctAnswer) {

    public static AnswerResponse from(QuizRepository.AnswerResult r) {
        Verdict verdict = r.getVerdict() == null ? null : DbEnum.fromValue(Verdict.class, r.getVerdict());
        return new AnswerResponse(verdict, r.getExplanation(),
                r.getCorrectAnswer());
    }
}
