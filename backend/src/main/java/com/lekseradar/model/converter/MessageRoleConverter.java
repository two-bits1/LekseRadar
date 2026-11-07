package com.lekseradar.model.converter;

import com.lekseradar.model.enums.MessageRoleEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** MessageRoleEnum ↔ значения PostgreSQL-enum chat_role ('student', 'tutor', 'system'). */
@Converter
public class MessageRoleConverter implements AttributeConverter<MessageRoleEnum, String> {

    @Override
    public String convertToDatabaseColumn(MessageRoleEnum role) {
        return role == null ? null : role.value();
    }

    @Override
    public MessageRoleEnum convertToEntityAttribute(String value) {
        return value == null ? null : MessageRoleEnum.fromValue(value);
    }
}
