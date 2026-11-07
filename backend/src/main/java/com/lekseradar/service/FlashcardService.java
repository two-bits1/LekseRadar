package com.lekseradar.service;

import com.lekseradar.dto.dashboard.FlashcardStatsResponseDTO;
import com.lekseradar.dto.flashcard.DeckResponseDTO;
import com.lekseradar.dto.flashcard.FlashcardResponseDTO;
import com.lekseradar.dto.flashcard.ReviewResponseDTO;
import com.lekseradar.dto.quiz.StartQuizRequestDTO;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.ExerciseRepository;
import com.lekseradar.repository.FlashcardRepository;
import com.lekseradar.repository.QuizSetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Флеш-карты = вопросы квизов в режиме повторения (V9): лицевая сторона — вопрос, обратная — ответ и пояснение.
 * Новые карточки составляет ИИ тем же промптом quiz_generator; ошибки в квизах база добавляет в колоду сама.
 */
@Service
public class FlashcardService {
    private final FlashcardRepository cards;
    private final QuizSetRepository sets;
    private final ExerciseRepository exercises;
    private final QuizGeneratorService generator;

    public FlashcardService(FlashcardRepository cards, QuizSetRepository sets, ExerciseRepository exercises,
                            QuizGeneratorService generator) {
        this.cards = cards;
        this.sets = sets;
        this.exercises = exercises;
        this.generator = generator;
    }

    /** @param dueOnly true — только те, что пора повторить */
    public List<FlashcardResponseDTO> list(Long userId, boolean dueOnly) {
        return (dueOnly ? cards.findDueCards(userId) : cards.findCards(userId)).stream()
                .map(FlashcardResponseDTO::from).toList();
    }

    public FlashcardStatsResponseDTO stats(Long userId) {
        return cards.findStats(userId).map(FlashcardStatsResponseDTO::from).orElse(FlashcardStatsResponseDTO.EMPTY);
    }

    /** Оценка 0–5 по SM-2 → когда показать карточку снова. */
    public ReviewResponseDTO review(Long userId, Long exerciseId, int grade) {
        if (!exercises.existsById(exerciseId)) throw new NotFoundException("Карточка не найдена");
        return ReviewResponseDTO.from(cards.review(userId, exerciseId, grade));
    }

    /** ИИ составляет вопросы по предмету и теме (или берётся готовый набор) → все в колоду. */
    public DeckResponseDTO generate(UserEntity user, StartQuizRequestDTO req) {
        QuizGeneratorService.SetResult set = generator.findOrGenerate(user, req);
        int added = cards.addQuizSet(user.getId(), set.quizSetId());
        return new DeckResponseDTO(set.quizSetId(), set.chatId(), set.generated(), added,
                cardsOf(user.getId(), set.quizSetId()));
    }

    /** Кнопка «Учить карточками» после квиза. */
    public DeckResponseDTO addSet(Long userId, Long setId) {
        if (!sets.existsById(setId)) throw new NotFoundException("Набор вопросов не найден");
        int added = cards.addQuizSet(userId, setId);
        return new DeckResponseDTO(setId, null, false, added, cardsOf(userId, setId));
    }

    private List<FlashcardResponseDTO> cardsOf(Long userId, Long setId) {
        return cards.findCards(userId).stream().filter(c -> Objects.equals(c.getQuizSetId(), setId))
                .map(FlashcardResponseDTO::from).toList();
    }
}
