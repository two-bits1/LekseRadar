package com.lekseradar.model.converter;

import com.lekseradar.model.enums.ChatModeEnum;
import jakarta.persistence.Converter;

/** ChatModeEnum ↔ PostgreSQL-enum chat_mode. */
@Converter(autoApply = true)
public class ChatModeConverter extends DbEnumConverter<ChatModeEnum> {
    public ChatModeConverter() {
        super(ChatModeEnum.class);
    }
}
