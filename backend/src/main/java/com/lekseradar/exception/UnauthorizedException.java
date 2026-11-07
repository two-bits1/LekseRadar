package com.lekseradar.exception;

import org.springframework.http.HttpStatus;

/** 401: неверные учётные данные. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
