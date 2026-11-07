package com.lekseradar.dto.user;

import com.lekseradar.model.User;

import java.time.Instant;

public record UserResponse(Long id, String email, String displayName, String locale, String timezone,
                           Instant createdAt, Instant lastLoginAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getLocale(), u.getTimezone(),
                u.getCreatedAt(), u.getLastLoginAt());
    }
}
