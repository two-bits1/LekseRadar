package com.lekseradar.model.converter;

import com.lekseradar.model.enums.ExerciseLevel;
import jakarta.persistence.Converter;

/** ExerciseLevel ↔ PostgreSQL-enum exercise_level. */
@Converter(autoApply = true)
public class ExerciseLevelConverter extends DbEnumConverter<ExerciseLevel> {
    public ExerciseLevelConverter() {
        super(ExerciseLevel.class);
    }
}
