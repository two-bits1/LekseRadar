package com.lekseradar.model.converter;

import com.lekseradar.model.enums.AnswerTypeEnum;
import jakarta.persistence.Converter;

/** AnswerTypeEnum ↔ PostgreSQL-enum answer_type. */
@Converter(autoApply = true)
public class AnswerTypeConverter extends DbEnumConverter<AnswerTypeEnum> {
    public AnswerTypeConverter() {
        super(AnswerTypeEnum.class);
    }
}
