package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code material_source}: откуда учебный материал (seed в базе есть, но не используется). */
public enum MaterialSourceEnum implements DbEnum {
    UPLOAD("upload"),
    URL("url"),
    PLATFORM("platform"),
    TEACHER("teacher"),
    SEED("seed");

    private final String value;

    MaterialSourceEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static MaterialSourceEnum fromValue(String value) {
        return DbEnum.fromValue(MaterialSourceEnum.class, value);
    }
}
