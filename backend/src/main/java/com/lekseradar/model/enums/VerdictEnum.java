package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code verdict}: результат проверки ответа. */
public enum VerdictEnum implements DbEnum {
    CORRECT("correct"),
    INCORRECT("incorrect"),
    PARTIAL("partial");

    private final String value;

    VerdictEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static VerdictEnum fromValue(String value) {
        return DbEnum.fromValue(VerdictEnum.class, value);
    }
}
