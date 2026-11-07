package com.lekseradar.exception;

import org.springframework.http.HttpStatus;

/** 400: запрос корректен по формату, но не может быть выполнен. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
