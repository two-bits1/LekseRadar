package com.lekseradar.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Подставляет в параметр контроллера текущего пользователя ({@link com.lekseradar.model.User}):
 * {@code public UserResponse me(@CurrentUser User user)}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {}
