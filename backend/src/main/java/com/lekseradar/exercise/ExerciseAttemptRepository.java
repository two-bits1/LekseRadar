package com.lekseradar.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, Long> {
    List<ExerciseAttempt> findByExerciseIdOrderByCreatedAtDesc(Long exerciseId);

    List<ExerciseAttempt> findByExerciseIdInOrderByCreatedAtDesc(Collection<Long> exerciseIds);

    List<ExerciseAttempt> findByUserIdAndCreatedAtAfter(Long userId, Instant after);

    /** Строки: subject, attempts, correct, avgScore. */
    @Query("""
            select e.subject, count(a), sum(case when a.correct = true then 1 else 0 end), avg(a.score)
            from ExerciseAttempt a, Exercise e
            where a.exerciseId = e.id and a.userId = :userId
            group by e.subject order by count(a) desc
            """)
    List<Object[]> statsBySubject(Long userId);

    /** Одна строка: attempts, correct, avgScore. */
    @Query("""
            select count(a), sum(case when a.correct = true then 1 else 0 end), avg(a.score)
            from ExerciseAttempt a where a.userId = :userId
            """)
    List<Object[]> totals(Long userId);

    @Query("select a.createdAt from ExerciseAttempt a where a.userId = :userId order by a.createdAt desc")
    List<Instant> activityTimes(Long userId);
}
