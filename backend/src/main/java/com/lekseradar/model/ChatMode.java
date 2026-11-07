package com.lekseradar.model;

/** PostgreSQL-тип {@code chat_mode}: вид чата. Задаётся при создании и не меняется. */
public enum ChatMode implements DbEnum {
    TUTOR("tutor"),
    QUIZ("quiz");

    private final String value;

    ChatMode(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static ChatMode fromValue(String value) {
        return DbEnum.fromValue(ChatMode.class, value);
    }
}
