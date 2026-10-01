package com.habitquest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt only uses the first 72 bytes of a password, so cap it there.
        @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password,
        @NotBlank @Size(max = 50) String displayName,
        // IANA zone such as "Asia/Yangon"; optional, defaults to UTC.
        @Size(max = 64) String timezone
) {
    public RegisterRequest {
        // Trim before validation runs, so a pasted " me@mail.com " still counts as a valid email.
        email = email == null ? null : email.trim();
    }
}
