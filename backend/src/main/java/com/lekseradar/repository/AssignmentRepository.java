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

    // Всё, что ученик сделал по заданиям платформы, которую он отключает. Подзапросы считаются заново в каждом
    // запросе, поэтому порядок в removePlatformAssignments важен: сначала то, что ищется через квизы, потом квизы.
    /** Задания ученика с этой платформы. */
    String PLATFORM_ASSIGNMENTS = "SELECT id FROM assignments WHERE user_id = :userId AND source = CAST(:source AS platform)";
    /** Квизы ученика по этим заданиям. */
    String PLATFORM_QUIZZES = "SELECT id FROM quizzes WHERE user_id = :userId AND assignment_id IN (" + PLATFORM_ASSIGNMENTS + ")";
    /** Чаты по этим заданиям и чаты, из которых сделаны эти квизы. */
    String PLATFORM_CHATS = "SELECT id FROM chat_sessions WHERE user_id = :userId AND (assignment_id IN ("
            + PLATFORM_ASSIGNMENTS + ") OR id IN (SELECT chat_session_id FROM quizzes WHERE id IN (" + PLATFORM_QUIZZES + ")))";
    /** Вопросы-карточки этих квизов (если тот же набор вопросов не используется в другом квизе ученика) и свои упражнения по заданиям. */
    String PLATFORM_EXERCISES = "SELECT id FROM exercises WHERE quiz_set_id IN ("
            + "SELECT quiz_set_id FROM quizzes WHERE id IN (" + PLATFORM_QUIZZES + ") AND quiz_set_id IS NOT NULL "
            + "EXCEPT SELECT quiz_set_id FROM quizzes WHERE user_id = :userId AND quiz_set_id IS NOT NULL AND id NOT IN ("
            + PLATFORM_QUIZZES + ")) OR (user_id = :userId AND assignment_id IN (" + PLATFORM_ASSIGNMENTS + "))";

    /** История (user_events): отметки «сдано», начатые квизы, ответы в них, чаты, повторения этих карточек. */
    @Modifying
    @Query(value = "DELETE FROM user_events WHERE user_id = :userId AND ("
            + "(ref_table = 'assignments' AND ref_id IN (" + PLATFORM_ASSIGNMENTS + "))"
            + " OR (ref_table = 'quizzes' AND ref_id IN (" + PLATFORM_QUIZZES + "))"
            + " OR (payload ->> 'quiz_id' IN (SELECT CAST(id AS text) FROM quizzes WHERE id IN (" + PLATFORM_QUIZZES + ")))"
            + " OR (ref_table = 'chat_sessions' AND ref_id IN (" + PLATFORM_CHATS + "))"
            + " OR (ref_table = 'exercises' AND ref_id IN (" + PLATFORM_EXERCISES + ")))", nativeQuery = true)
    int deletePlatformEvents(@Param("userId") Long userId, @Param("source") String source);

    /** Ответы ученика на эти вопросы вне квизов (ответы в квизах удалятся вместе с квизами). */
    @Modifying
    @Query(value = "DELETE FROM exercise_attempts WHERE user_id = :userId AND exercise_id IN (" + PLATFORM_EXERCISES + ")",
            nativeQuery = true)
    int deletePlatformAttempts(@Param("userId") Long userId, @Param("source") String source);

    /** Карточки ученика из этих квизов. */
    @Modifying
    @Query(value = "DELETE FROM flashcard_progress WHERE user_id = :userId AND exercise_id IN (" + PLATFORM_EXERCISES + ")",
            nativeQuery = true)
    int deletePlatformFlashcards(@Param("userId") Long userId, @Param("source") String source);

    /** Чаты с сообщениями (сообщения удаляются каскадом). */
    @Modifying
    @Query(value = "DELETE FROM chat_sessions WHERE id IN (" + PLATFORM_CHATS + ")", nativeQuery = true)
    int deletePlatformChats(@Param("userId") Long userId, @Param("source") String source);

    /** Квизы с вопросами и ответами (каскадом). */
    @Modifying
    @Query(value = "DELETE FROM quizzes WHERE id IN (" + PLATFORM_QUIZZES + ")", nativeQuery = true)
    int deletePlatformQuizzes(@Param("userId") Long userId, @Param("source") String source);

    /** Свои упражнения ученика по этим заданиям, если они не стоят в чужом квизе. */
    @Modifying
    @Query(value = "DELETE FROM exercises e WHERE e.user_id = :userId AND e.assignment_id IN (" + PLATFORM_ASSIGNMENTS + ")"
            + " AND NOT EXISTS (SELECT 1 FROM quiz_questions qq WHERE qq.exercise_id = e.id)", nativeQuery = true)
    int deletePlatformExercises(@Param("userId") Long userId, @Param("source") String source);

    /** Файлы ДЗ с платформы ({@code materials.source = 'platform'}), привязанные к её заданиям ученика. */
    @Modifying
    @Query(value = """
            DELETE FROM materials m USING assignments a
            WHERE m.assignment_id = a.id AND m.source = 'platform'
              AND a.user_id = :userId AND a.source = CAST(:source AS platform)
            """, nativeQuery = true)
    int deletePlatformFiles(@Param("userId") Long userId, @Param("source") String source);

    /** Задания ученика с этой платформы (напоминания по ним удаляются каскадом). */
    @Modifying
    @Query(value = "DELETE FROM assignments WHERE user_id = :userId AND source = CAST(:source AS platform)",
            nativeQuery = true)
    int deleteBySource(@Param("userId") Long userId, @Param("source") String source);

    /**
     * Ученик отключил платформу: удалить её задания и всё, что сделано по ним, — чаты, квизы, карточки, ответы,
     * историю, файлы ДЗ. Тогда они пропадают и с главной, и из карточек, и из прогресса. Вызывать в транзакции.
     * Обычный SQL, без функции базы: работает и на базе, куда V26 (очистка старых остатков) ещё не накатили.
     * Общие наборы вопросов (quiz_sets, их exercises) не удаляются: ими пользуются и другие ученики.
     * @param source значение enum'а в базе ("classroom", "canvas"…)
     * @return сколько заданий удалено
     */
    default int removePlatformAssignments(Long userId, String source) {
        deletePlatformEvents(userId, source);
        deletePlatformAttempts(userId, source);
        deletePlatformFlashcards(userId, source);
        deletePlatformChats(userId, source);
        deletePlatformQuizzes(userId, source);
        deletePlatformExercises(userId, source);
        deletePlatformFiles(userId, source);
        return deleteBySource(userId, source);
    }

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
