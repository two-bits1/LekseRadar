package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code platform}: учебная платформа, откуда пришло ДЗ. */
public enum PlatformEnum implements DbEnum {
    CANVAS("canvas"),
    TEAMS("teams"),
    ITSLEARNING("itslearning"),
    INSCHOOL("inschool"),
    /** Google Classroom (вход через Google, нужна миграция V22 с этим значением в типе platform). */
    CLASSROOM("classroom");

    private final String value;

    PlatformEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static PlatformEnum fromValue(String value) {
        return DbEnum.fromValue(PlatformEnum.class, value);
    }
}
