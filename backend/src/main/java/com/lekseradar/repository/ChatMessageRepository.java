package com.lekseradar.repository;

import com.lekseradar.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    /** Реплика из вида chat_context. */
    interface ContextMessage {
        String getRole();
        String getContent();
    }

    /** История для ИИ: последние 10 реплик чата без системных, по порядку (вид chat_context). */
    @Query(value = """
            SELECT CAST(role AS text) AS role, content
            FROM chat_context
            WHERE session_id = :sessionId
            ORDER BY created_at
            """, nativeQuery = true)
    List<ContextMessage> findContext(@Param("sessionId") Long sessionId);
}
