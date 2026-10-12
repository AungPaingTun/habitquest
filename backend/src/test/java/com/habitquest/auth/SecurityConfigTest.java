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

    @Test
    void isProduction_appEnvProd_isTrueOnAnyHost() {
        assertThat(SecurityConfig.isProduction("prod", null)).isTrue();
        assertThat(SecurityConfig.isProduction("PROD", null)).isTrue();
        assertThat(SecurityConfig.isProduction("production", null)).isTrue();
    }

    @Test
    void isProduction_herokuDyno_isTrue() {
        assertThat(SecurityConfig.isProduction(null, "web.1")).isTrue();
    }

    @Test
    void isProduction_localDev_isFalse() {
        assertThat(SecurityConfig.isProduction(null, null)).isFalse();
        assertThat(SecurityConfig.isProduction("dev", null)).isFalse();
    }

    private static final String ORIGIN_KEY = "0123456789abcdef0123456789abcdef";

    @Test
    void checkedOriginKey_missingLocally_turnsCheckOff() {
        assertThat(SecurityConfig.checkedOriginKey(null, false)).isNull();
        assertThat(SecurityConfig.checkedOriginKey("", false)).isNull();
    }

    @Test
    void checkedOriginKey_missingInProduction_refusesToStart() {
        assertThatThrownBy(() -> SecurityConfig.checkedOriginKey("", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ORIGIN_KEY is not set");
    }

    @Test
    void checkedOriginKey_tooShort_isRejected() {
        assertThatThrownBy(() -> SecurityConfig.checkedOriginKey("short", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void checkedOriginKey_longKeyInProduction_isAllowed() {
        assertThat(SecurityConfig.checkedOriginKey(ORIGIN_KEY, true)).isEqualTo(ORIGIN_KEY);
    }
}
