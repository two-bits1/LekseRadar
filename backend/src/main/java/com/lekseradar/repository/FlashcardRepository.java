package com.lekseradar.repository;

import com.lekseradar.model.FlashcardProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Колода карточек: таблица flashcard_progress, вид flashcards и функции V9. */
public interface FlashcardRepository extends JpaRepository<FlashcardProgressEntity, FlashcardProgressEntity.Key> {

    /** Карточка: лицевая сторона — вопрос, обратная — ответ и пояснение (вид flashcards). */
    interface Card {
        Long getExerciseId();
        Long getQuizSetId();
        String getSubject();
        String getTopic();
        String getFront();
        String getBack();
        String getExplanation();
        Instant getNextReviewAt();
        Integer getIntervalDays();
        Integer getReviews();
        Boolean getDue();
    }

    String CARD_COLUMNS = """
            exercise_id AS exerciseId, quiz_set_id AS quizSetId, subject, topic, front, back, explanation,
            next_review_at AS nextReviewAt, interval_days AS intervalDays, reviews, due
            """;

    @Query(value = "SELECT " + CARD_COLUMNS + " FROM flashcards WHERE user_id = :userId ORDER BY next_review_at",
            nativeQuery = true)
    List<Card> findCards(@Param("userId") Long userId);

    /** Что повторять сейчас. */
    @Query(value = "SELECT " + CARD_COLUMNS + " FROM flashcards WHERE user_id = :userId AND due ORDER BY next_review_at",
            nativeQuery = true)
    List<Card> findDueCards(@Param("userId") Long userId);

    /** Весь квиз в колоду: сколько карточек добавлено. */
    @Transactional
    @Query(value = "SELECT add_quiz_set_to_flashcards(:userId, :setId)", nativeQuery = true)
    Integer addQuizSet(@Param("userId") Long userId, @Param("setId") Long setId);

    interface ReviewResult {
        Instant getNextReviewAt();
        Integer getIntervalDays();
    }

    /** Оценка 0–5 (SM-2) → когда показать снова. */
    @Transactional
    @Query(value = """
            SELECT next_review_at AS nextReviewAt, interval_days AS intervalDays
            FROM review_flashcard(:userId, :exerciseId, :grade)
            """, nativeQuery = true)
    ReviewResult review(@Param("userId") Long userId, @Param("exerciseId") Long exerciseId,
                        @Param("grade") Integer grade);

    interface Stats {
        Long getCards();
        Long getDueNow();
        Long getLearned();
        Long getReviews();
    }

    @Query(value = """
            SELECT cards, due_now AS dueNow, learned, reviews FROM user_flashcard_stats WHERE user_id = :userId
            """, nativeQuery = true)
    Optional<Stats> findStats(@Param("userId") Long userId);
}
