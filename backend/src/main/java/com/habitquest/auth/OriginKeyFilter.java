package com.habitquest.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Only lets /api requests through if they came via the Cloudflare Worker, which adds a secret X-Origin-Key header.
 * The Heroku URL is public, so without this anyone could call it directly and skip Cloudflare's login rate limit.
 * If no key is configured (local dev, tests), every request is allowed.
 */
public class OriginKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Origin-Key";

    private final byte[] expectedKey;

    public OriginKeyFilter(String originKey) {
        this.expectedKey = originKey == null || originKey.isBlank() ? null : originKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return expectedKey == null || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader(HEADER);
        // MessageDigest.isEqual takes the same time for any wrong key, so timing can't reveal the key byte by byte.
        if (given == null || !MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), expectedKey)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
