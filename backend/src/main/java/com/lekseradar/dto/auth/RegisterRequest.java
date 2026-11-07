package com.lekseradar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email(message = "Некорректный email") String email,
        @NotBlank @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов") String password,
        @NotBlank @Size(max = 100) String displayName) {}
