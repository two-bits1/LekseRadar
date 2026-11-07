package com.lekseradar.service.google;

import com.lekseradar.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * Метка {@code state} для входа через Google: связывает возврат со страницы Google с учеником, который нажал
 * «Подключить», и защищает от подсовывания чужого кода (CSRF). Подписана HMAC-SHA256 и ничего не хранит —
 * переживает перезапуск сервера. Формат {@code userId.выдана.случайное.подпись}, живёт 10 минут.
 * Ключ выводится из JWT_SECRET, но отличается от него: метку нельзя выдать за токен входа и наоборот.
 */
@Component
public class OAuthStates {
    static final Duration TTL = Duration.ofMinutes(10);
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final byte[] key;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    @Autowired
    public OAuthStates(AppConfig cfg) {
        this(cfg.jwt().secret(), Clock.systemUTC());
    }

    public OAuthStates(String secret, Clock clock) {
        this.key = hmac(secret.getBytes(StandardCharsets.UTF_8), "lekseradar-oauth-state".getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String issue(Long userId) {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        String payload = userId + "." + clock.instant().getEpochSecond() + "." + ENC.encodeToString(nonce);
        return payload + "." + ENC.encodeToString(hmac(key, payload.getBytes(StandardCharsets.UTF_8)));
    }

    /** true — метка выдана этому ученику нашим сервером и ей меньше 10 минут. */
    public boolean valid(String state, Long userId) {
        if (state == null) return false;
        int dot = state.lastIndexOf('.');
        if (dot < 0) return false;
        String payload = state.substring(0, dot);
        String[] parts = payload.split("\\.");
        try {
            byte[] sig = DEC.decode(state.substring(dot + 1));
            return parts.length == 3
                    && MessageDigest.isEqual(sig, hmac(key, payload.getBytes(StandardCharsets.UTF_8)))
                    && parts[0].equals(String.valueOf(userId))
                    && clock.instant().getEpochSecond() - Long.parseLong(parts[1]) <= TTL.toSeconds();
        } catch (IllegalArgumentException e) {   // не base64 / не число
            return false;
        }
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
