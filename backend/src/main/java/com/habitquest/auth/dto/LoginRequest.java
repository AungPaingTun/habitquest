package com.habitquest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank String email, @NotBlank @Size(max = 255) String password) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
