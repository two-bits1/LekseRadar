package com.lekseradar.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsecureSecretsTest {
    @Test
    void randomSecretIsAccepted() {
        assertFalse(InsecureSecrets.isKnown("q3Jx9vB2…random-generated-secret-value-1234567890"));
        assertDoesNotThrow(() -> InsecureSecrets.reject("JWT_SECRET", "another-random-value-that-is-long-enough"));
    }

    @Test
    void publicDefaultIsRejected() {
        // старое значение по умолчанию собирается из частей, чтобы целиком оно в коде не встречалось
        String old = String.join("-", "local", "dev", "collector", "key");
        assertTrue(InsecureSecrets.isKnown(old));
        assertThrows(IllegalStateException.class, () -> InsecureSecrets.reject("COLLECTOR_KEY", old));
    }
}
