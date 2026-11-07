package com.lekseradar.model.converter;

import com.lekseradar.model.enums.QuizStatusEnum;
import jakarta.persistence.Converter;

/** QuizStatusEnum ↔ PostgreSQL-enum quiz_status. */
@Converter(autoApply = true)
public class QuizStatusConverter extends DbEnumConverter<QuizStatusEnum> {
    public QuizStatusConverter() {
        super(QuizStatusEnum.class);
    }
}
