package com.lekseradar.dto.quiz;

import com.lekseradar.model.enums.DbEnum;
import com.lekseradar.model.enums.VerdictEnum;
import com.lekseradar.repository.QuizRepository;

/** Результат ответа: вердикт, пояснение и правильный ответ (показывается после ответа). */
public record AnswerResponseDTO(VerdictEnum verdict, String explanation, String correctAnswer) {

    public static AnswerResponseDTO from(QuizRepository.AnswerResult r) {
        VerdictEnum verdict = r.getVerdict() == null ? null : DbEnum.fromValue(VerdictEnum.class, r.getVerdict());
        return new AnswerResponseDTO(verdict, r.getExplanation(),
                r.getCorrectAnswer());
    }
}
