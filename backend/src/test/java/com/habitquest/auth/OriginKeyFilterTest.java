package com.habitquest.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OriginKeyFilterTest {

    private static final String KEY = "k3y-from-wrangler-secret";

    private record Result(int status, boolean passedThrough) {}

    private static Result run(String configuredKey, String path, String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        if (headerValue != null) {
            request.addHeader(OriginKeyFilter.HEADER, headerValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        new OriginKeyFilter(configuredKey).doFilter(request, response, chain);
        return new Result(response.getStatus(), chain.getRequest() != null);
    }

    @Test
    void noKeyConfigured_allowsEveryRequest() throws Exception {
        assertThat(run(null, "/api/auth/login", null).passedThrough()).isTrue();
        assertThat(run("", "/api/auth/login", null).passedThrough()).isTrue();
    }

    @Test
    void correctKey_isAllowed() throws Exception {
        assertThat(run(KEY, "/api/auth/login", KEY).passedThrough()).isTrue();
    }

    @Test
    void missingKey_isForbidden() throws Exception {
        Result r = run(KEY, "/api/auth/login", null);
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.passedThrough()).isFalse();
    }

    @Test
    void wrongKey_isForbidden() throws Exception {
        Result r = run(KEY, "/api/habits", "guess");
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.passedThrough()).isFalse();
    }

    @Test
    void healthCheck_doesNotNeedKey() throws Exception {
        assertThat(run(KEY, "/actuator/health", null).passedThrough()).isTrue();
    }
}
