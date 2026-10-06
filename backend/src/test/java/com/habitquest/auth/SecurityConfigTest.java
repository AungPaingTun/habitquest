package com.habitquest.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    private static final String DEV_SECRET = "local-dev-secret-change-me-in-production-0123456789";

    @Test
    void checkedSecret_devSecretLocally_isAllowed() {
        assertThat(SecurityConfig.checkedSecret(DEV_SECRET, false)).hasSize(DEV_SECRET.length());
    }

    @Test
    void checkedSecret_devSecretInProduction_refusesToStart() {
        assertThatThrownBy(() -> SecurityConfig.checkedSecret(DEV_SECRET, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is not set");
    }

    @Test
    void checkedSecret_realSecretInProduction_isAllowed() {
        String secret = "x7Kq2mP9vR4tW8yB3nF6hJ1cL5sD0gZa";
        assertThat(SecurityConfig.checkedSecret(secret, true)).hasSize(32);
    }

    @Test
    void checkedSecret_tooShort_isRejected() {
        assertThatThrownBy(() -> SecurityConfig.checkedSecret("short", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
