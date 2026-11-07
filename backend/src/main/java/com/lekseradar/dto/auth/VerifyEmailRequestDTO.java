package com.lekseradar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** {@code POST /auth/register/verify}: код из письма. */
public record VerifyEmailRequestDTO(
        @NotBlank @Email(message = "Некорректный email") String email,
        @NotBlank @Pattern(regexp = "\\s*\\d{6}\\s*", message = "Код — 6 цифр из письма") String code) {}
