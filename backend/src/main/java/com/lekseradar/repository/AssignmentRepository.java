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

    /** Сколько заданий добавлено и обновлено одним {@link #syncAssignments}. */
    interface SyncCounts {
        Integer getAdded();
        Integer getUpdated();
    }

    /**
     * Все задания из ответа collect() одним вызовом (функция базы V21): UPSERT, «сдано» на сайте не откатывается.
     * @param items JSON-массив {@code CollectResult.assignments} как есть
     */
    @Query(value = "SELECT added, updated FROM sync_assignments(:userId, CAST(:items AS jsonb))", nativeQuery = true)
    SyncCounts syncAssignments(@Param("userId") Long userId, @Param("items") String items);

    /**
     * Ученик отключил платформу: удалить её задания и файлы ДЗ с неё (функция базы V26). Чаты и квизы остаются
     * без ссылки на задание (ON DELETE SET NULL), напоминания удаляются. Вызывать в транзакции.
     * @param source значение enum'а в базе ("classroom", "canvas"…)
     * @return сколько заданий удалено
     */
    @Query(value = "SELECT remove_platform_assignments(:userId, CAST(:source AS platform))", nativeQuery = true)
    int removePlatformAssignments(@Param("userId") Long userId, @Param("source") String source);

    /** Несданные задания со сроком в ближайшие {@code within} (например "24 hours"), о которых ещё не было письма (V25). */
    @Query(value = "SELECT * FROM assignments_to_remind(CAST(:within AS interval))", nativeQuery = true)
    List<AssignmentEntity> findToRemind(@Param("within") String within);

    /** Несданные задания ученика со сроком до {@code until}, включая просроченные: для пробного письма. */
    @Query(value = """
            SELECT * FROM assignments
            WHERE user_id = :userId AND status IN ('todo', 'overdue') AND due_at IS NOT NULL AND due_at <= :until
            ORDER BY due_at
            """, nativeQuery = true)
    List<AssignmentEntity> findOpenDueBefore(@Param("userId") Long userId, @Param("until") Instant until);

    /** Прикрепить текст файла ДЗ (для промпта тьютора): id материала. */
    @Query(value = "SELECT attach_homework_file(:assignmentId, :text, :fileName, :mimeType, :storageKey)",
            nativeQuery = true)
    Long attachHomeworkFile(@Param("assignmentId") Long assignmentId, @Param("text") String text,
                            @Param("fileName") String fileName, @Param("mimeType") String mimeType,
                            @Param("storageKey") String storageKey);
}
