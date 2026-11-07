package com.lekseradar.model;

/** PostgreSQL-тип {@code quiz_status}: состояние прохождения квиза. */
public enum QuizStatus implements DbEnum {
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    ABANDONED("abandoned");

    private final String value;

    QuizStatus(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static QuizStatus fromValue(String value) {
        return DbEnum.fromValue(QuizStatus.class, value);
    }
}
