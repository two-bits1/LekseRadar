package com.lekseradar.dto.flashcard;

import java.util.List;

/**
 * Карточки добавлены в колоду ({@code POST /flashcards/generate}, {@code POST /quiz-sets/{id}/flashcards}).
 *
 * @param chatId    квиз-чат, где ИИ составил вопросы (null — набор уже был в базе)
 * @param generated true — ИИ составил вопросы сейчас
 * @param added     сколько карточек новых (уже бывшие в колоде не считаются)
 * @param cards     все карточки этого набора
 */
public record DeckResponseDTO(Long quizSetId, Long chatId, boolean generated, int added,
                              List<FlashcardResponseDTO> cards) {}
