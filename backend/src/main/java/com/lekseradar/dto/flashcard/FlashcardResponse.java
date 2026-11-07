package com.lekseradar.dto.flashcard;

import com.lekseradar.repository.FlashcardRepository;

import java.time.Instant;

/**
 * Флеш-карта ({@code GET /flashcards/due}): лицевая сторона — вопрос, обратная — ответ и пояснение.
 */
public record FlashcardResponse(Long exerciseId, String subject, String topic, String front, String back,
                                String explanation, Instant nextReviewAt) {

    public static FlashcardResponse from(FlashcardRepository.Card c) {
        return new FlashcardResponse(c.getExerciseId(), c.getSubject(), c.getTopic(), c.getFront(), c.getBack(),
                c.getExplanation(), c.getNextReviewAt());
    }
}
