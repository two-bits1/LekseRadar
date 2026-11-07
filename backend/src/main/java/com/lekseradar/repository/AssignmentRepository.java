package com.lekseradar.repository;

import com.lekseradar.model.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, Long> {

    /** Список ДЗ: сначала с дедлайном по возрастанию, без дедлайна в конце. */
    @Query("SELECT a FROM AssignmentEntity a WHERE a.userId = :userId ORDER BY a.dueAt ASC NULLS LAST, a.id")
    List<AssignmentEntity> findForUser(@Param("userId") Long userId);

    Optional<AssignmentEntity> findByIdAndUserId(Long id, Long userId);

    /**
     * Одно задание из ответа collect(): новое добавляется, уже известное обновляется.
     * source / status — значения enum'ов в базе ("canvas", "todo"…), kind — assignment/quiz/exam/discussion/task.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO assignments (user_id, source, external_id, title, course, due_at, status, url, description, kind)
            VALUES (:userId, CAST(:source AS platform), :externalId, :title, :course, :dueAt,
                    CAST(:status AS assignment_status), :url, :description, :kind)
            ON CONFLICT (user_id, source, external_id) DO UPDATE
            SET title = EXCLUDED.title, course = EXCLUDED.course, due_at = EXCLUDED.due_at,
                status = EXCLUDED.status, url = EXCLUDED.url, description = EXCLUDED.description,
                kind = EXCLUDED.kind, updated_at = now()
            """, nativeQuery = true)
    int upsert(@Param("userId") Long userId, @Param("source") String source, @Param("externalId") String externalId,
               @Param("title") String title, @Param("course") String course, @Param("dueAt") Instant dueAt,
               @Param("status") String status, @Param("url") String url, @Param("description") String description,
               @Param("kind") String kind);

    /** Прикрепить текст файла ДЗ (для промпта тьютора): id материала. */
    @Query(value = "SELECT attach_homework_file(:assignmentId, :text, :fileName, :mimeType, :storageKey)",
            nativeQuery = true)
    Long attachHomeworkFile(@Param("assignmentId") Long assignmentId, @Param("text") String text,
                            @Param("fileName") String fileName, @Param("mimeType") String mimeType,
                            @Param("storageKey") String storageKey);
}
