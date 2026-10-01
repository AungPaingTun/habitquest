package com.habitquest.user;

import java.time.Instant;

/** What the API returns about a user. Never includes the password hash. */
public record UserResponse(Long id, String email, String displayName, String timezone, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getTimezone(), user.getCreatedAt());
    }
}
