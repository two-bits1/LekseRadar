package com.lekseradar.dto.user;

import com.lekseradar.model.UserEntity;

import java.time.Instant;

public record UserResponseDTO(Long id, String email, String displayName, String locale, String timezone,
                           Instant createdAt, Instant lastLoginAt) {

    public static UserResponseDTO from(UserEntity u) {
        return new UserResponseDTO(u.getId(), u.getEmail(), u.getDisplayName(), u.getLocale(), u.getTimezone(),
                u.getCreatedAt(), u.getLastLoginAt());
    }
}
