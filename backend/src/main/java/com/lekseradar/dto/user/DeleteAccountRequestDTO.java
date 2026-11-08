package com.lekseradar.dto.user;

import jakarta.validation.constraints.NotBlank;

/** Удаление аккаунта: текущий пароль, чтобы чужой открытый браузер не удалил аккаунт одним кликом. */
public record DeleteAccountRequestDTO(@NotBlank String password) {}
