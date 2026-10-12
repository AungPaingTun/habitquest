package com.habitquest.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the real server with an origin key, to prove OriginKeyFilter is wired into the security chain
 * and that Tomcat + Spring don't route an encoded path around it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.origin-key=" + OriginKeyIntegrationTest.KEY)
class OriginKeyIntegrationTest {

    static final String KEY = "integration-test-origin-key-0123456789";
    private static final String LOGIN_BODY = "{\"email\":\"nobody@example.test\",\"password\":\"wrong-password\"}";

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private int post(String path, String key) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(LOGIN_BODY));
        if (key != null) {
            request.header(OriginKeyFilter.HEADER, key);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void loginWithoutKey_isForbidden() throws Exception {
        assertThat(post("/api/auth/login", null)).isEqualTo(403);
    }

    @Test
    void loginWithEncodedPathAndNoKey_isForbidden() throws Exception {
        assertThat(post("/%61pi/auth/login", null)).isEqualTo(403);
    }

    @Test
    void loginWithKey_reachesLogin() throws Exception {
        // 401 = wrong password, so the request got past the origin check to the login code.
        assertThat(post("/api/auth/login", KEY)).isEqualTo(401);
    }

    @Test
    void healthCheck_doesNotNeedKey() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
    }
}
