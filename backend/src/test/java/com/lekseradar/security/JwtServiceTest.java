package com.lekseradar.security;

import com.lekseradar.config.AppConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "test-secret-test-secret-test-secret-123";

    private static JwtService service(String secret, long ttlHours) {
        return new JwtService(new AppConfig(new AppConfig.Jwt(secret, ttlHours), null, null));
    }

    @Test
    void issuedTokenContainsUserId() {
        JwtService jwt = service(SECRET, 1);
        assertThat(jwt.parseUserId(jwt.issue(42L))).contains(42L);
    }

    @Test
    void rejectsForeignExpiredAndBrokenTokens() {
        JwtService jwt = service(SECRET, 1);
        String foreign = service("another-secret-another-secret-another-1", 1).issue(42L);
        String expired = service(SECRET, -1).issue(42L);

        assertThat(jwt.parseUserId(foreign)).isEmpty();
        assertThat(jwt.parseUserId(expired)).isEmpty();
        assertThat(jwt.parseUserId("not-a-jwt")).isEmpty();
    }

    @Test
    void shortSecretIsRejectedAtStartup() {
        assertThatThrownBy(() -> service("short", 1)).isInstanceOf(IllegalStateException.class);
    }
}
