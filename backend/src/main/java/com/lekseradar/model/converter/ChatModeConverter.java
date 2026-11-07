package com.lekseradar.model.converter;

import com.lekseradar.model.enums.ChatMode;
import jakarta.persistence.Converter;

/** ChatMode ↔ PostgreSQL-enum chat_mode. */
@Converter(autoApply = true)
public class ChatModeConverter extends DbEnumConverter<ChatMode> {
    public ChatModeConverter() {
        super(ChatMode.class);
    }
}
