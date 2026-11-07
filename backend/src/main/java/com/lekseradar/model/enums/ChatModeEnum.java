package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code chat_mode}: вид чата. Задаётся при создании и не меняется. */
public enum ChatModeEnum implements DbEnum {
    TUTOR("tutor"),
    QUIZ("quiz");

    private final String value;

    ChatModeEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static ChatModeEnum fromValue(String value) {
        return DbEnum.fromValue(ChatModeEnum.class, value);
    }
}
