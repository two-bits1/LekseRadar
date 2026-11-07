package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code answer_type}: exact — ответ проверяет база, free — ИИ. */
public enum AnswerTypeEnum implements DbEnum {
    EXACT("exact"),
    FREE("free");

    private final String value;

    AnswerTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static AnswerTypeEnum fromValue(String value) {
        return DbEnum.fromValue(AnswerTypeEnum.class, value);
    }
}
