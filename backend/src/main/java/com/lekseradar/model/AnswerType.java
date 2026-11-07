package com.lekseradar.model;

/** PostgreSQL-тип {@code answer_type}: exact — ответ проверяет база, free — ИИ. */
public enum AnswerType implements DbEnum {
    EXACT("exact"),
    FREE("free");

    private final String value;

    AnswerType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static AnswerType fromValue(String value) {
        return DbEnum.fromValue(AnswerType.class, value);
    }
}
