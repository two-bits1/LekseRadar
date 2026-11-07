package com.lekseradar.dto.auth;

import com.lekseradar.dto.user.UserResponse;

/** @param token JWT, передавайте его в заголовке {@code Authorization: Bearer <token>} */
public record AuthResponse(String token, UserResponse user) {}
