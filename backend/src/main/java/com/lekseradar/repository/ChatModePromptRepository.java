package com.lekseradar.repository;

import com.lekseradar.model.ChatModePrompt;
import org.springframework.data.jpa.repository.JpaRepository;

/** Ключ — значение режима: "tutor" или "quiz". */
public interface ChatModePromptRepository extends JpaRepository<ChatModePrompt, String> {
}
