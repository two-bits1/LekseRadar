package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/** Таблица {@code chat_mode_prompts} (V13): какой промпт у какого режима чата (tutor → tutor, quiz → quiz_generator). */
@Entity
@Immutable
@Table(name = "chat_mode_prompts")
public class ChatModePromptEntity {
    /** Значение chat_mode как текст: JPA не применяет конвертеры к @Id. */
    @Id
    @Column(name = "mode", columnDefinition = "chat_mode")
    private String mode;

    @Column(name = "prompt_key")
    private String promptKey;

    protected ChatModePromptEntity() {}

    public ChatModeEnum getMode() { return ChatModeEnum.fromValue(mode); }
    public String getPromptKey() { return promptKey; }
}
