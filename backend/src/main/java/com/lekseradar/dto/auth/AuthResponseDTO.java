package com.lekseradar.dto.auth;

import com.lekseradar.dto.user.UserResponseDTO;

/** @param token JWT, передавайте его в заголовке {@code Authorization: Bearer <token>} */
public record AuthResponseDTO(String token, UserResponseDTO user) {}
