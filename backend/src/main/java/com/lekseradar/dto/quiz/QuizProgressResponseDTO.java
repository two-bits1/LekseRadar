package com.lekseradar.dto.quiz;

import com.lekseradar.repository.QuizRepository;

import java.math.BigDecimal;
import java.time.Instant;

/** Прогресс по предмету и теме ({@code GET /quizzes/progress}, вид user_quiz_progress). */
public record QuizProgressResponseDTO(String subject, String topic, long quizzesCompleted, long quizzesInProgress,
                                      BigDecimal bestScore, BigDecimal lastScore, Instant lastActivity) {

    public static QuizProgressResponseDTO from(QuizRepository.QuizProgress p) {
        return new QuizProgressResponseDTO(p.getSubject(), p.getTopic(),
                p.getQuizzesCompleted() == null ? 0 : p.getQuizzesCompleted(),
                p.getQuizzesInProgress() == null ? 0 : p.getQuizzesInProgress(),
                p.getBestScore(), p.getLastScore(), p.getLastActivity());
    }
}
