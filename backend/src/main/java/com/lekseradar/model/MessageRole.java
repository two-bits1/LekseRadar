package com.lekseradar.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/** PostgreSQL-тип {@code chat_role}: автор сообщения в чате. В JSON — "student" / "tutor" / "system". */
public enum MessageRole {
    STUDENT("student"),
    TUTOR("tutor"),
    SYSTEM("system");

    private final String value;

    MessageRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static MessageRole fromValue(String value) {
        return Arrays.stream(values()).filter(r -> r.value.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown chat_role: " + value));
    }
}
