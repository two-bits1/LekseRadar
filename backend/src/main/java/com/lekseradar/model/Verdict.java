package com.lekseradar.model;

/** PostgreSQL-тип {@code verdict}: результат проверки ответа. */
public enum Verdict implements DbEnum {
    CORRECT("correct"),
    INCORRECT("incorrect"),
    PARTIAL("partial");

    private final String value;

    Verdict(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static Verdict fromValue(String value) {
        return DbEnum.fromValue(Verdict.class, value);
    }
}
