package com.habitquest.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Binds the app.jwt.* settings from application.yml. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
