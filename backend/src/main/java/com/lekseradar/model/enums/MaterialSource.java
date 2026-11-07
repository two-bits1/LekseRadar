package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code material_source}: откуда учебный материал (seed в базе есть, но не используется). */
public enum MaterialSource implements DbEnum {
    UPLOAD("upload"),
    URL("url"),
    PLATFORM("platform"),
    TEACHER("teacher"),
    SEED("seed");

    private final String value;

    MaterialSource(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static MaterialSource fromValue(String value) {
        return DbEnum.fromValue(MaterialSource.class, value);
    }
}
