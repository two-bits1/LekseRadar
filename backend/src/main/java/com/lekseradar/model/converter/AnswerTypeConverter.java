package com.lekseradar.model.converter;

import com.lekseradar.model.enums.AnswerType;
import jakarta.persistence.Converter;

/** AnswerType ↔ PostgreSQL-enum answer_type. */
@Converter(autoApply = true)
public class AnswerTypeConverter extends DbEnumConverter<AnswerType> {
    public AnswerTypeConverter() {
        super(AnswerType.class);
    }
}
