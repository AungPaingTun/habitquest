package com.habitquest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Enter your email") String email,
        @NotBlank(message = "Enter your password") @Size(max = 255, message = "Password is too long") String password
) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
