package com.lekseradar.model;

/** PostgreSQL-тип {@code platform}: учебная платформа, откуда пришло ДЗ. */
public enum Platform implements DbEnum {
    CANVAS("canvas"),
    TEAMS("teams"),
    ITSLEARNING("itslearning"),
    INSCHOOL("inschool");

    private final String value;

    Platform(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static Platform fromValue(String value) {
        return DbEnum.fromValue(Platform.class, value);
    }
}
