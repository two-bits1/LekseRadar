package com.lekseradar.exception;

import org.springframework.http.HttpStatus;

/** 409: конфликт с существующими данными (например, email уже занят). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
