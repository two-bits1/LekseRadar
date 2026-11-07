package com.lekseradar.dto.dashboard;

import com.lekseradar.repository.FlashcardRepository;

/** Колода: всего карточек, к повторению сейчас, выучено, всего повторений. */
public record FlashcardStatsResponseDTO(long cards, long dueNow, long learned, long reviews) {

    public static final FlashcardStatsResponseDTO EMPTY = new FlashcardStatsResponseDTO(0, 0, 0, 0);

    public static FlashcardStatsResponseDTO from(FlashcardRepository.Stats s) {
        return new FlashcardStatsResponseDTO(n(s.getCards()), n(s.getDueNow()), n(s.getLearned()), n(s.getReviews()));
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
