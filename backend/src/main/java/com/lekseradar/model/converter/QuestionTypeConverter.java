package com.lekseradar.model.converter;

import com.lekseradar.model.QuestionType;
import jakarta.persistence.Converter;

/** QuestionType ↔ TEXT exercises.question_type. */
@Converter(autoApply = true)
public class QuestionTypeConverter extends DbEnumConverter<QuestionType> {
    public QuestionTypeConverter() {
        super(QuestionType.class);
    }
}
