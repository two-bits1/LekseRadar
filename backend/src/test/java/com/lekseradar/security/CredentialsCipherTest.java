package com.lekseradar.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CredentialsCipherTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private static JsonNode creds() throws Exception {
        return JSON.readTree("{\"base_url\": \"https://uio.instructure.com\", \"token\": \"secret-token-123\"}");
    }

    @Test
    void roundTrip() throws Exception {
        CredentialsCipher cipher = new CredentialsCipher(KEY, JSON);
        byte[] enc = cipher.encrypt(creds());
        assertEquals(creds(), cipher.decrypt(enc));
        assertFalse(new String(enc, StandardCharsets.ISO_8859_1).contains("secret-token-123"));
    }

    @Test
    void sameJsonGivesDifferentBytes() throws Exception {
        CredentialsCipher cipher = new CredentialsCipher(KEY, JSON);
        assertFalse(java.util.Arrays.equals(cipher.encrypt(creds()), cipher.encrypt(creds())));
    }

    @Test
    void otherKeyFails() throws Exception {
        byte[] enc = new CredentialsCipher(KEY, JSON).encrypt(creds());
        assertThrows(IllegalStateException.class, () -> new CredentialsCipher(OTHER_KEY, JSON).decrypt(enc));
    }

    @Test
    void tamperedByteFails() throws Exception {
        CredentialsCipher cipher = new CredentialsCipher(KEY, JSON);
        byte[] enc = cipher.encrypt(creds());
        enc[enc.length - 5] ^= 1;
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(enc));
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(new byte[5]));
    }

    @Test
    void badKeyStopsStartup() {
        assertThrows(IllegalStateException.class, () -> new CredentialsCipher("", JSON));
        assertThrows(IllegalStateException.class, () -> new CredentialsCipher("not base64!", JSON));
        assertThrows(IllegalStateException.class,
                () -> new CredentialsCipher(Base64.getEncoder().encodeToString(new byte[16]), JSON));
    }
}
