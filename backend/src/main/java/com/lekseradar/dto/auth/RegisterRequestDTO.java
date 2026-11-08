package com.lekseradar.dto.auth;

import com.lekseradar.dto.validation.BcryptSafe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank @Email(message = "Некорректный email") String email,
        @NotBlank @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов") @BcryptSafe String password,
        @NotBlank @Size(max = 100) String displayName,
        /** Принимаю условия и политику конфиденциальности; мне 13 лет или родитель согласен (GDPR, V27). */
        @NotNull(message = "Нужно принять условия и политику конфиденциальности")
        @AssertTrue(message = "Нужно принять условия и политику конфиденциальности") Boolean acceptTerms) {}
