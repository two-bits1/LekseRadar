package com.lekseradar.dto.validation;

import com.lekseradar.dto.auth.RegisterRequestDTO;
import com.lekseradar.dto.user.ChangePasswordRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/** Пароль длиннее 72 байт раньше давал 500 (BCrypt бросает исключение), теперь — 400 с понятным текстом. */
class BcryptSafeTest {
    private static final Validator V = Validation.buildDefaultValidatorFactory().getValidator();

    private static boolean passwordInvalid(String pw) {
        return V.validate(new RegisterRequestDTO("ola@example.com", pw, "Ola")).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void up_to_72_bytes_is_fine_and_bcrypt_accepts_it() {
        String latin72 = "a".repeat(72);
        String cyrillic36 = "п".repeat(36);   // 72 байта
        assertFalse(passwordInvalid(latin72));
        assertFalse(passwordInvalid(cyrillic36));
        assertDoesNotThrow(() -> new BCryptPasswordEncoder(4).encode(cyrillic36));
    }

    @Test
    void longer_than_72_bytes_is_400_not_500() {
        assertTrue(passwordInvalid("a".repeat(73)));
        assertTrue(passwordInvalid("п".repeat(37)));   // 74 байта, хотя всего 37 символов
        assertTrue(V.validate(new ChangePasswordRequestDTO("old-password", "a".repeat(80))).stream()
                .anyMatch(v -> v.getMessage().contains("72 байт")));
    }
}
