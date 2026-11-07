package com.lekseradar.exception;

import org.springframework.http.HttpStatus;

/** 429: слишком много запросов (перебор пароля, частые запросы к ИИ, частое обновление ДЗ). */
public class TooManyRequestsException extends ApiException {
    /** Секунды для заголовка {@code Retry-After}, null — без заголовка. */
    private final Long retryAfterSeconds;

    public TooManyRequestsException(String message) {
        this(message, null);
    }

    public TooManyRequestsException(String message, Long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
