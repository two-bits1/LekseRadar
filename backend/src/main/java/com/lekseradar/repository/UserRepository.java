package com.lekseradar.repository;

import com.lekseradar.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Дашборд (вид user_dashboard). */
    interface Dashboard {
        Long getHomeworkTodo();
        Long getHomeworkOverdue();
        Long getHomeworkDone();
        Long getQuizzesCompleted();
        BigDecimal getAvgQuizScore();
        Long getExercisesAnswered();
        Long getQuestionsAsked();
        Long getActiveDays30();
    }

    @Query(value = """
            SELECT homework_todo AS homeworkTodo, homework_overdue AS homeworkOverdue, homework_done AS homeworkDone,
                   quizzes_completed AS quizzesCompleted, avg_quiz_score AS avgQuizScore,
                   exercises_answered AS exercisesAnswered, questions_asked AS questionsAsked,
                   active_days_30 AS activeDays30
            FROM user_dashboard WHERE user_id = :userId
            """, nativeQuery = true)
    Optional<Dashboard> findDashboard(@Param("userId") Long userId);

    /** Освоение по темам (вид user_topic_progress). */
    interface TopicProgressResponseDTO {
        String getTopic();
        Long getAttempts();
        Long getCorrect();
        Long getPartial();
        BigDecimal getMasteryPct();
        Instant getLastActivity();
    }

    @Query(value = """
            SELECT topic, attempts, correct, partial, mastery_pct AS masteryPct, last_activity AS lastActivity
            FROM user_topic_progress WHERE user_id = :userId ORDER BY last_activity DESC
            """, nativeQuery = true)
    List<TopicProgressResponseDTO> findTopicProgress(@Param("userId") Long userId);

    /** Активность по дням (вид user_daily_activity). */
    interface DailyActivity {
        LocalDate getDay();
        Long getEvents();
        Long getExercises();
        Long getQuizzes();
    }

    @Query(value = """
            SELECT day, events, exercises, quizzes FROM user_daily_activity WHERE user_id = :userId ORDER BY day
            """, nativeQuery = true)
    List<DailyActivity> findDailyActivity(@Param("userId") Long userId);

    /**
     * GDPR (ст. 15 и 20): все данные ученика одним JSON. Без хеша пароля и без зашифрованных доступов к платформам.
     */
    @Query(value = """
            SELECT jsonb_build_object(
              'exported_at', now(),
              'profile', (SELECT to_jsonb(u) - 'password_hash' FROM users u WHERE u.id = :userId),
              'platforms', (SELECT coalesce(jsonb_agg(to_jsonb(c) - 'credentials_enc' ORDER BY c.id), '[]')
                            FROM platform_connections c WHERE c.user_id = :userId),
              'assignments', (SELECT coalesce(jsonb_agg(to_jsonb(a) ORDER BY a.id), '[]') FROM assignments a WHERE a.user_id = :userId),
              'chats', (SELECT coalesce(jsonb_agg(to_jsonb(s) || jsonb_build_object('messages',
                          (SELECT coalesce(jsonb_agg(to_jsonb(m) ORDER BY m.created_at, m.id), '[]')
                           FROM chat_messages m WHERE m.session_id = s.id)) ORDER BY s.id), '[]')
                        FROM chat_sessions s WHERE s.user_id = :userId),
              'quizzes', (SELECT coalesce(jsonb_agg(to_jsonb(q) ORDER BY q.id), '[]') FROM quizzes q WHERE q.user_id = :userId),
              'answers', (SELECT coalesce(jsonb_agg(to_jsonb(x) || jsonb_build_object('question', e.statement) ORDER BY x.id), '[]')
                          FROM exercise_attempts x JOIN exercises e ON e.id = x.exercise_id WHERE x.user_id = :userId),
              'flashcards', (SELECT coalesce(jsonb_agg(to_jsonb(f) || jsonb_build_object('question', e.statement) ORDER BY f.exercise_id), '[]')
                             FROM flashcard_progress f JOIN exercises e ON e.id = f.exercise_id WHERE f.user_id = :userId),
              'own_exercises', (SELECT coalesce(jsonb_agg(to_jsonb(e) ORDER BY e.id), '[]') FROM exercises e WHERE e.user_id = :userId),
              'materials', (SELECT coalesce(jsonb_agg(to_jsonb(m) ORDER BY m.id), '[]') FROM materials m WHERE m.owner_id = :userId),
              'reminders', (SELECT coalesce(jsonb_agg(to_jsonb(r) ORDER BY r.id), '[]') FROM reminders r WHERE r.user_id = :userId),
              'achievements', (SELECT coalesce(jsonb_agg(to_jsonb(a) ORDER BY a.unlocked_at), '[]') FROM user_achievements a WHERE a.user_id = :userId),
              'activity', (SELECT coalesce(jsonb_agg(to_jsonb(ev) ORDER BY ev.id), '[]') FROM user_events ev WHERE ev.user_id = :userId)
            )::text
            """, nativeQuery = true)
    String exportJson(@Param("userId") Long userId);

    /** Удалить ученика; всё его база удаляет каскадом (ON DELETE CASCADE / SET NULL у общих наборов вопросов). */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM users WHERE id = :userId", nativeQuery = true)
    int deleteAccount(@Param("userId") Long userId);

    /** Аккаунты без входа с {@code before}: срок хранения (RetentionScheduler). */
    @Query(value = "SELECT id FROM users WHERE coalesce(last_login_at, created_at) < :before ORDER BY id", nativeQuery = true)
    List<Long> findInactiveSince(@Param("before") Instant before);
}
