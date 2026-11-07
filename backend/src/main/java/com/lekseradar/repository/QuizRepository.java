package com.lekseradar.repository;

import com.lekseradar.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<Quiz> findByIdAndUserId(Long id, Long userId);

    /** Начать квиз по набору или продолжить незаконченный: id прохождения. */
    @Transactional
    @Query(value = "SELECT start_quiz(:userId, :setId)", nativeQuery = true)
    Long start(@Param("userId") Long userId, @Param("setId") Long setId);

    /** Вопрос для экрана квиза, без правильного ответа (вид quiz_questions_public). */
    interface PublicQuestion {
        Integer getPosition();
        Long getExerciseId();
        String getQuestionType();
        String getQuestion();
        /** JSON-массив вариантов или null. */
        String getOptions();
    }

    @Query(value = """
            SELECT position, exercise_id AS exerciseId, question_type AS questionType, question,
                   CAST(options AS text) AS options
            FROM quiz_questions_public WHERE quiz_id = :quizId ORDER BY position
            """, nativeQuery = true)
    List<PublicQuestion> findPublicQuestions(@Param("quizId") Long quizId);

    /** Результат save_quiz_answer. */
    interface AnswerResult {
        Long getAttemptId();
        String getVerdict();
        String getExplanation();
        String getCorrectAnswer();
    }

    /**
     * Ответ на вопрос. Для single/multiple_choice база проверяет сама (verdict = null);
     * для text передать вердикт ИИ (correct / incorrect / partial) и пояснение.
     */
    @Transactional
    @Query(value = """
            SELECT attempt_id AS attemptId, CAST(verdict AS text) AS verdict, explanation, correct_answer AS correctAnswer
            FROM save_quiz_answer(:quizId, :position, :answer, CAST(:verdict AS verdict), :explanation)
            """, nativeQuery = true)
    AnswerResult saveAnswer(@Param("quizId") Long quizId, @Param("position") Integer position,
                            @Param("answer") String answer, @Param("verdict") String verdict,
                            @Param("explanation") String explanation);

    /** Завершить квиз: балл 0–100 (partial = 0,5). */
    @Transactional
    @Query(value = "SELECT finish_quiz(:quizId)", nativeQuery = true)
    BigDecimal finish(@Param("quizId") Long quizId);

    /** Прогресс по предметам и темам (вид user_quiz_progress). */
    interface QuizProgress {
        String getSubject();
        String getTopic();
        Long getQuizzesCompleted();
        Long getQuizzesInProgress();
        BigDecimal getBestScore();
        BigDecimal getLastScore();
        Instant getLastActivity();
    }

    @Query(value = """
            SELECT subject, topic, quizzes_completed AS quizzesCompleted, quizzes_in_progress AS quizzesInProgress,
                   best_score AS bestScore, last_score AS lastScore, last_activity AS lastActivity
            FROM user_quiz_progress WHERE user_id = :userId ORDER BY last_activity DESC
            """, nativeQuery = true)
    List<QuizProgress> findProgress(@Param("userId") Long userId);
}
