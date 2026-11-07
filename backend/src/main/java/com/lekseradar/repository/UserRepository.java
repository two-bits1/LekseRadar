package com.lekseradar.repository;

import com.lekseradar.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
