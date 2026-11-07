package com.lekseradar.model.converter;

import com.lekseradar.model.MessageRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** MessageRole ↔ значения PostgreSQL-enum chat_role ('student', 'tutor', 'system'). */
@Converter
public class MessageRoleConverter implements AttributeConverter<MessageRole, String> {

    @Override
    public String convertToDatabaseColumn(MessageRole role) {
        return role == null ? null : role.value();
    }

    @Override
    public MessageRole convertToEntityAttribute(String value) {
        return value == null ? null : MessageRole.fromValue(value);
    }
}
