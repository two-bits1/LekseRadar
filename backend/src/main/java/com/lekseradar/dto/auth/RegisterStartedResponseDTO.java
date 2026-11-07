package com.lekseradar.dto.auth;

/**
 * Ответ {@code POST /auth/register}: аккаунт ещё не создан, на email ушёл код.
 *
 * @param expiresInSeconds сколько секунд действует код
 */
public record RegisterStartedResponseDTO(String email, long expiresInSeconds) {}
