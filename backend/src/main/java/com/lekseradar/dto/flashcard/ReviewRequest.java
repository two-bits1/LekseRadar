package com.lekseradar.dto.flashcard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** {@code POST /flashcards/{exerciseId}/review}: оценка 0 (не помню) – 5 (легко). */
public record ReviewRequest(@Min(0) @Max(5) int grade) {}
