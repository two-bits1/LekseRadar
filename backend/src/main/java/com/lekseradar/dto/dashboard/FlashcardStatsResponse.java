package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.FlashcardRepository;

/** Колода: всего карточек, к повторению сейчас, выучено, всего повторений. */
public record FlashcardStatsResponse(long cards, long dueNow, long learned, long reviews) {

    public static final FlashcardStatsResponse EMPTY = new FlashcardStatsResponse(0, 0, 0, 0);

    public static FlashcardStatsResponse from(FlashcardRepository.Stats s) {
        return new FlashcardStatsResponse(n(s.getCards()), n(s.getDueNow()), n(s.getLearned()), n(s.getReviews()));
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
