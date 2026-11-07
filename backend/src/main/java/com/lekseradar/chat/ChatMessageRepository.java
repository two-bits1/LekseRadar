package com.lekseradar.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    List<ChatMessage> findBySessionIdOrderByCreatedAtDescIdDesc(Long sessionId, Pageable page);

    void deleteBySessionId(Long sessionId);

    @Query("""
            select count(m) from ChatMessage m, ChatSession s
            where m.sessionId = s.id and s.userId = :userId and m.role = com.lekseradar.chat.ChatMessage.Role.USER
            """)
    long countUserMessages(Long userId);
}
