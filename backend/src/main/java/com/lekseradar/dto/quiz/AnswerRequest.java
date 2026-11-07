package com.lekseradar.dto.quiz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /quizzes/{id}/answers}.
 *
 * @param answer single_choice и text — строка; multiple_choice — JSON-массив строкой: ["A","C"]
 */
public record AnswerRequest(@Min(1) int position, @NotBlank @Size(max = 8000) String answer) {}
