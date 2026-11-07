package com.lekseradar.model.converter;

import com.lekseradar.model.enums.QuestionTypeEnum;
import jakarta.persistence.Converter;

/** QuestionTypeEnum ↔ TEXT exercises.question_type. */
@Converter(autoApply = true)
public class QuestionTypeConverter extends DbEnumConverter<QuestionTypeEnum> {
    public QuestionTypeConverter() {
        super(QuestionTypeEnum.class);
    }
}
