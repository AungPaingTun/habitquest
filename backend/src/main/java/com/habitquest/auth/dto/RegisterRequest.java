package com.habitquest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Enter your email") @Email(message = "Enter a valid email address")
        @Size(max = 255, message = "Email is too long") String email,
        // BCrypt only uses the first 72 bytes of a password, so cap it there.
        @NotBlank(message = "Choose a password")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password,
        @NotBlank(message = "Enter your name") @Size(max = 50, message = "Name can be at most 50 characters") String displayName,
        // IANA zone such as "Asia/Yangon"; optional, defaults to UTC.
        @Size(max = 64, message = "Time zone is too long") String timezone
) {
    public RegisterRequest {
        // Trim before validation runs, so a pasted " me@mail.com " still counts as a valid email.
        email = email == null ? null : email.trim();
    }
}
