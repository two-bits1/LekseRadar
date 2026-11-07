package com.lekseradar.repository;

import com.lekseradar.model.ChatMode;
import com.lekseradar.model.ChatModePrompt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatModePromptRepository extends JpaRepository<ChatModePrompt, ChatMode> {
}
