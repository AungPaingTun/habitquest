package com.habitquest.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, @Value("${app.origin-key:}") String originKey)
            throws Exception {
        http
                // Rejects requests that didn't come through the Cloudflare Worker (see OriginKeyFilter).
                .addFilterBefore(new OriginKeyFilter(checkedOriginKey(originKey, isProduction())),
                        BearerTokenAuthenticationFilter.class)
                // No cookies/sessions: the client sends "Authorization: Bearer <token>" on every request,
                // so CSRF protection (which defends cookie-based sessions) isn't needed.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated())
                // Validates the Bearer token on every request using the JwtDecoder bean below.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return NimbusJwtEncoder.withSecretKey(signingKey(properties)).build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        return NimbusJwtDecoder.withSecretKey(signingKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static SecretKey signingKey(JwtProperties properties) {
        return new SecretKeySpec(checkedSecret(properties.secret(), isProduction()), "HmacSHA256");
    }

    private static boolean isProduction() {
        return isProduction(System.getenv("APP_ENV"), System.getenv("DYNO"));
    }

    /**
     * Without ORIGIN_KEY the Heroku URL accepts requests that skipped Cloudflare. In production, refuse to start
     * rather than silently run unprotected. A short key could be guessed, so require 32+ characters everywhere.
     */
    static String checkedOriginKey(String key, boolean production) {
        if (key == null || key.isBlank()) {
            if (production) {
                throw new IllegalStateException("ORIGIN_KEY is not set: refusing to start without the Cloudflare origin check");
            }
            return null;
        }
        if (key.length() < 32) {
            throw new IllegalStateException("app.origin-key must be at least 32 characters");
        }
        return key;
    }

    /**
     * APP_ENV=prod (or production) marks production on any host. DYNO (set by Heroku on every dyno) is a fallback,
     * so the dev-secret guard keeps working even if APP_ENV is forgotten on Heroku.
     */
    static boolean isProduction(String appEnv, String dyno) {
        return "prod".equalsIgnoreCase(appEnv) || "production".equalsIgnoreCase(appEnv) || dyno != null;
    }

    /**
     * The local-dev default in application.yml is public (the repo is open source), so anyone could sign tokens with it.
     * If JWT_SECRET ever goes missing in production, refuse to start rather than silently accept forged logins.
     */
    static byte[] checkedSecret(String secret, boolean production) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        if (production && secret.startsWith("local-dev-secret")) {
            throw new IllegalStateException("JWT_SECRET is not set: refusing to start with the public dev secret");
        }
        return bytes;
    }
}
