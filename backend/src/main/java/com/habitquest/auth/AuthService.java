package com.habitquest.auth;

import com.habitquest.auth.dto.AuthResponse;
import com.habitquest.auth.dto.LoginRequest;
import com.habitquest.auth.dto.RegisterRequest;
import com.habitquest.common.ApiException;
import com.habitquest.user.User;
import com.habitquest.user.UserRepository;
import com.habitquest.user.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // Compared against when the email doesn't exist, so a failed login takes the same time either way
    // and response timing doesn't reveal which emails are registered.
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw emailTaken(null);
        }
        if (!fitsBcrypt(request.password())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password is too long (non-English characters count as 2–4 letters)");
        }

        User user;
        try {
            user = users.save(new User(
                    email,
                    passwordEncoder.encode(request.password()),
                    request.displayName().trim(),
                    validTimezone(request.timezone())));
        } catch (DataIntegrityViolationException e) {
            // Two sign-ups with the same email at the same moment both pass the check above;
            // the database's UNIQUE constraint stops the second one.
            throw emailTaken(e);
        }

        return new AuthResponse(tokenService.issueToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalizeEmail(request.email())).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;

        // Same message for "no such email", "wrong password" and "impossible password",
        // so attackers can't probe for accounts.
        boolean valid = fitsBcrypt(request.password()) && passwordEncoder.matches(request.password(), hash);
        if (!valid || user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return new AuthResponse(tokenService.issueToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }

    private static ApiException emailTaken(Throwable cause) {
        ApiException ex = new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        if (cause != null) ex.initCause(cause);
        return ex;
    }

    /** BCrypt only accepts 72 bytes. Burmese/Thai/emoji characters take 3–4 bytes each in UTF-8. */
    private static boolean fitsBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String validTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return "UTC";
        }
        // Only region zones like "Asia/Yangon". Fixed offsets ("+06:30", "GMT+8") ignore daylight saving,
        // which would shift "today" by an hour for some users.
        String id = timezone.trim();
        if (!ZoneId.getAvailableZoneIds().contains(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown timezone: " + timezone);
        }
        return id;
    }
}
