package com.lekseradar.exception;

import org.springframework.http.HttpStatus;

/** 502: ИИ недоступен или вернул ответ, который нельзя разобрать. */
public class AiServiceException extends ApiException {
    public AiServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
