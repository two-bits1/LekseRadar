package com.lekseradar.model.converter;

import com.lekseradar.model.enums.ExerciseLevelEnum;
import jakarta.persistence.Converter;

/** ExerciseLevelEnum ↔ PostgreSQL-enum exercise_level. */
@Converter(autoApply = true)
public class ExerciseLevelConverter extends DbEnumConverter<ExerciseLevelEnum> {
    public ExerciseLevelConverter() {
        super(ExerciseLevelEnum.class);
    }
}
