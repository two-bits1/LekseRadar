package com.lekseradar.exception;

import com.lekseradar.dto.common.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/** Превращает любые исключения в {@link ErrorResponse} с правильным HTTP-статусом. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return build(e.getStatus(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(),
                        f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
                        (a, b) -> a));
        return build(HttpStatus.BAD_REQUEST, "Проверьте введённые данные", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "Некорректное тело запроса", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "Некорректное значение параметра '" + e.getName() + "'", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST, "Не указан параметр '" + e.getParameterName() + "'", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "Адрес не найден: /" + e.getResourcePath(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Метод " + e.getMethod() + " не поддерживается для этого адреса", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Ожидается Content-Type: application/json", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleOther(Exception e) {
        log.error("Unhandled error", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message, fields, Instant.now()));
    }
}
