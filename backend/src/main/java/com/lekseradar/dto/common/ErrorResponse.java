package com.lekseradar.dto.common;

import java.time.Instant;
import java.util.Map;

/**
 * Единый формат ошибки API.
 *
 * @param fields ошибки валидации по полям (только для 400), иначе null
 */
public record ErrorResponse(int status, String message, Map<String, String> fields, Instant timestamp) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, null, Instant.now());
    }
}
