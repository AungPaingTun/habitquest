package com.habitquest.auth.dto;

import com.habitquest.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
