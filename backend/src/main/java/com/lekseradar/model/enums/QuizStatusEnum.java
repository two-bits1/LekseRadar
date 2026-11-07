package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code quiz_status}: состояние прохождения квиза. */
public enum QuizStatusEnum implements DbEnum {
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    ABANDONED("abandoned");

    private final String value;

    QuizStatusEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static QuizStatusEnum fromValue(String value) {
        return DbEnum.fromValue(QuizStatusEnum.class, value);
    }
}
