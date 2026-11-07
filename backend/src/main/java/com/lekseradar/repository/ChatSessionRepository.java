package com.lekseradar.repository;

import com.lekseradar.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<ChatSession> findByUserIdAndArchivedOrderByUpdatedAtDesc(Long userId, boolean archived);

    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);

    /**
     * Новый чат через функцию базы: mode = "tutor" или "quiz", предмет создаётся сам по названию.
     * subject, topic, title, assignmentId можно не передавать (null). Возвращает id чата.
     */
    @Transactional
    @Query(value = """
            SELECT start_chat(:userId, CAST(:mode AS chat_mode), :subject, :topic, :title, :assignmentId)
            """, nativeQuery = true)
    Long startChat(@Param("userId") Long userId, @Param("mode") String mode, @Param("subject") String subject,
                   @Param("topic") String topic, @Param("title") String title,
                   @Param("assignmentId") Long assignmentId);

    /** Системный промпт режима чата (chat_system_prompt). Если промпт не загружен — ошибка базы. */
    interface SystemPrompt {
        Long getPromptId();
        String getPromptKey();
        Integer getVersion();
        String getContent();
        String getMode();
        Long getUserId();
        Long getSubjectId();
        String getSubject();
        String getTopic();
        String getLanguage();
    }

    @Query(value = """
            SELECT prompt_id AS promptId, prompt_key AS promptKey, version, content, CAST(mode AS text) AS mode,
                   user_id AS userId, subject_id AS subjectId, subject, topic, language
            FROM chat_system_prompt(:sessionId)
            """, nativeQuery = true)
    SystemPrompt systemPrompt(@Param("sessionId") Long sessionId);

    /** Значения для {{...}} в промпте одним JSON (chat_prompt_vars): language, subject, topic, homework_*… */
    @Query(value = "SELECT CAST(chat_prompt_vars(:sessionId) AS text)", nativeQuery = true)
    String promptVars(@Param("sessionId") Long sessionId);

    /** Ученик выбрал язык (после [[LANGUAGE=xx]] от тьютора). */
    @Transactional
    @Query(value = "SELECT CAST(set_chat_language(:sessionId, :language) AS text)", nativeQuery = true)
    String setLanguage(@Param("sessionId") Long sessionId, @Param("language") String language);
}
