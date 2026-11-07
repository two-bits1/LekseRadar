package com.lekseradar.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Java-enum, который хранится в PostgreSQL как ENUM или TEXT.
 * {@link #value()} — точное значение в базе и в JSON ("tutor", "in_progress"…).
 */
public interface DbEnum {
    @JsonValue
    String value();

    static <E extends Enum<E> & DbEnum> E fromValue(Class<E> type, String value) {
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.value().equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + type.getSimpleName() + ": " + value));
    }
}
