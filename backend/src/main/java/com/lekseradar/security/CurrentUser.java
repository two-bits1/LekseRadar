package com.lekseradar.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Подставляет в параметр контроллера текущего пользователя ({@link com.lekseradar.model.UserEntity}):
 * {@code public UserResponseDTO me(@CurrentUser UserEntity user)}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {}
