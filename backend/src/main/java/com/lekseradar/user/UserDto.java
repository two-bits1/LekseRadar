package com.lekseradar.user;

import java.time.Instant;

public record UserDto(Long id, String email, String displayName, Instant createdAt) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getCreatedAt());
    }
}
