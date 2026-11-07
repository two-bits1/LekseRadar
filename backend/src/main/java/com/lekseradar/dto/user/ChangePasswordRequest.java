package com.lekseradar.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов") String newPassword) {}
