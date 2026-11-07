package com.lekseradar.dto.user;

import com.lekseradar.dto.validation.BcryptSafe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов") @BcryptSafe String newPassword) {}
