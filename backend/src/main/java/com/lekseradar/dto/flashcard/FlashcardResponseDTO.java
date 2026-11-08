package com.lekseradar.dto.flashcard;

import com.lekseradar.repository.FlashcardRepository;

import java.time.Instant;

/**
 * Флеш-карта ({@code GET /flashcards}, {@code /flashcards/due}): лицевая сторона — вопрос, обратная — ответ и пояснение,
 * плюс прогресс SM-2, по которому фронт выбирает «выученные» и «трудные» карточки.
 */
public record FlashcardResponseDTO(Long exerciseId, String subject, String topic, String front, String back,
                                String explanation, Instant nextReviewAt,
                                int intervalDays, int reviews, int lapses, double ease, boolean due) {

    public static FlashcardResponseDTO from(FlashcardRepository.Card c) {
        return new FlashcardResponseDTO(c.getExerciseId(), c.getSubject(), c.getTopic(), c.getFront(), c.getBack(),
                c.getExplanation(), c.getNextReviewAt(),
                c.getIntervalDays() == null ? 0 : c.getIntervalDays(), c.getReviews() == null ? 0 : c.getReviews(),
                c.getLapses() == null ? 0 : c.getLapses(), c.getEase() == null ? 2.5 : c.getEase().doubleValue(),
                Boolean.TRUE.equals(c.getDue()));
    }
}
