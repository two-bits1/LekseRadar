package com.lekseradar.exception;

import org.springframework.http.HttpStatus;

/** Базовая ошибка API: HTTP-статус и понятное пользователю сообщение. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
