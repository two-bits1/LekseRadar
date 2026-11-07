package com.lekseradar.dto.flashcard;

import com.lekseradar.repository.FlashcardRepository;

import java.time.Instant;

/** Когда показать карточку снова. */
public record ReviewResponseDTO(Instant nextReviewAt, int intervalDays) {

    public static ReviewResponseDTO from(FlashcardRepository.ReviewResult r) {
        return new ReviewResponseDTO(r.getNextReviewAt(), r.getIntervalDays() == null ? 0 : r.getIntervalDays());
    }
}
