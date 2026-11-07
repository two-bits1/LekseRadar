package com.lekseradar.exception;

import org.springframework.http.HttpStatus;

/** 404: объект не найден или принадлежит другому пользователю. */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
