package com.lekseradar.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lekseradar.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Шифрует доступы к платформам ({@code platform_connections.credentials_enc}): JSON → AES-256-GCM.
 * Формат в базе: {@code nonce (12 байт) || ciphertext+tag (16 байт)}. Ключ — {@code CREDENTIALS_KEY} (base64, 32 байта).
 */
@Component
public class CredentialsCipher {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final ObjectMapper json;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public CredentialsCipher(AppConfig cfg, ObjectMapper json) {
        this(cfg.credentials() == null ? null : cfg.credentials().key(), json);
    }

    public CredentialsCipher(String base64Key, ObjectMapper json) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("app.credentials.key (CREDENTIALS_KEY) не задан: openssl rand -base64 32");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.credentials.key (CREDENTIALS_KEY) должен быть в base64: openssl rand -base64 32");
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("app.credentials.key (CREDENTIALS_KEY) должен быть ровно 32 байта, сейчас "
                    + bytes.length + ": openssl rand -base64 32");
        }
        InsecureSecrets.reject("CREDENTIALS_KEY", base64Key);
        this.key = new SecretKeySpec(bytes, "AES");
        this.json = json;
    }

    public byte[] encrypt(JsonNode creds) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(json.writeValueAsBytes(creds));
            byte[] out = new byte[NONCE_BYTES + encrypted.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_BYTES);
            System.arraycopy(encrypted, 0, out, NONCE_BYTES, encrypted.length);
            return out;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Не удалось зашифровать доступы платформы", e);
        }
    }

    /** @throws IllegalStateException чужой ключ или испорченные байты */
    public JsonNode decrypt(byte[] data) {
        if (data == null || data.length < NONCE_BYTES + TAG_BITS / 8) {
            throw new IllegalStateException("Доступы платформы повреждены");
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, NONCE_BYTES));
            return json.readTree(cipher.doFinal(data, NONCE_BYTES, data.length - NONCE_BYTES));
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Не удалось расшифровать доступы платформы");
        }
    }
}
