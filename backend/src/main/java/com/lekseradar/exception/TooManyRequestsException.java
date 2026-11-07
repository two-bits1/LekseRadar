package com.lekseradar.exception;

import org.springframework.http.HttpStatus;

/** 429: слишком много запросов (перебор пароля, частые запросы к ИИ). */
public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
