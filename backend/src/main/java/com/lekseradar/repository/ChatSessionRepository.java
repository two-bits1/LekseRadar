package com.lekseradar.repository;

import com.lekseradar.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<ChatSession> findByUserIdAndArchivedOrderByUpdatedAtDesc(Long userId, boolean archived);

    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);
}
