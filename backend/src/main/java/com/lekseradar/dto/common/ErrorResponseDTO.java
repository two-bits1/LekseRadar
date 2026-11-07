package com.lekseradar.dto.common;

import java.time.Instant;
import java.util.Map;

/**
 * Единый формат ошибки API.
 *
 * @param fields ошибки валидации по полям (только для 400), иначе null
 */
public record ErrorResponseDTO(int status, String message, Map<String, String> fields, Instant timestamp) {

    public static ErrorResponseDTO of(int status, String message) {
        return new ErrorResponseDTO(status, message, null, Instant.now());
    }
}
