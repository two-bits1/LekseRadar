package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code exercise_level}: уровень сложности квиза и чата. */
public enum ExerciseLevelEnum implements DbEnum {
    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced");

    private final String value;

    ExerciseLevelEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static ExerciseLevelEnum fromValue(String value) {
        return DbEnum.fromValue(ExerciseLevelEnum.class, value);
    }
}
