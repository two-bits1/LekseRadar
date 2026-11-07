package com.lekseradar.dto.flashcard;

import com.lekseradar.repository.FlashcardRepository;

import java.time.Instant;

/** Когда показать карточку снова. */
public record ReviewResponse(Instant nextReviewAt, int intervalDays) {

    public static ReviewResponse from(FlashcardRepository.ReviewResult r) {
        return new ReviewResponse(r.getNextReviewAt(), r.getIntervalDays() == null ? 0 : r.getIntervalDays());
    }
}
