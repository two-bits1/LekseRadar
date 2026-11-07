package com.lekseradar.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/** Таблица {@code chat_mode_prompts} (V13): какой промпт у какого режима чата (tutor → tutor, quiz → quiz_generator). */
@Entity
@Immutable
@Table(name = "chat_mode_prompts")
public class ChatModePrompt {
    /** Значение chat_mode как текст: JPA не применяет конвертеры к @Id. */
    @Id
    @Column(name = "mode", columnDefinition = "chat_mode")
    private String mode;

    @Column(name = "prompt_key")
    private String promptKey;

    protected ChatModePrompt() {}

    public ChatMode getMode() { return ChatMode.fromValue(mode); }
    public String getPromptKey() { return promptKey; }
}
