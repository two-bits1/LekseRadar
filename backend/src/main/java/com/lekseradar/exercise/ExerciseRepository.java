package com.lekseradar.exercise;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select e from Exercise e
            where e.userId = :userId and (:subject is null or e.subject = :subject)
            order by e.createdAt desc, e.id desc
            """)
    Page<Exercise> findForUser(Long userId, String subject, Pageable page);

    long countByUserId(Long userId);

    @Query("select distinct e.subject from Exercise e where e.userId = :userId order by e.subject")
    List<String> findSubjects(Long userId);
}
