package com.habitquest.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the logged-in user's id from the JWT (TokenService puts it in the "sub" claim). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
