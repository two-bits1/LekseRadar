package com.lekseradar.model.converter;

import com.lekseradar.model.QuizStatus;
import jakarta.persistence.Converter;

/** QuizStatus ↔ PostgreSQL-enum quiz_status. */
@Converter(autoApply = true)
public class QuizStatusConverter extends DbEnumConverter<QuizStatus> {
    public QuizStatusConverter() {
        super(QuizStatus.class);
    }
}
