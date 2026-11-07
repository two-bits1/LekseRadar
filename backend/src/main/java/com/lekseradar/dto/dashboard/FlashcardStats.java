package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.FlashcardRepository;

/** Колода: всего карточек, к повторению сейчас, выучено, всего повторений. */
public record FlashcardStats(long cards, long dueNow, long learned, long reviews) {

    public static final FlashcardStats EMPTY = new FlashcardStats(0, 0, 0, 0);

    public static FlashcardStats from(FlashcardRepository.Stats s) {
        return new FlashcardStats(n(s.getCards()), n(s.getDueNow()), n(s.getLearned()), n(s.getReviews()));
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
