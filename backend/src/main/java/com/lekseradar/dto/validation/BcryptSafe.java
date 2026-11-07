package com.lekseradar.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Пароль не длиннее 72 байт в UTF-8: больше BCrypt (Spring Security) не принимает и бросает исключение (было 500).
 * Русская буква — 2 байта, поэтому лимит в символах (100) для кириллицы не спасает.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BcryptSafe.Validator.class)
public @interface BcryptSafe {
    int MAX_BYTES = 72;

    String message() default "Пароль слишком длинный: не больше 72 байт (около 72 латинских или 36 русских букв)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<BcryptSafe, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext ctx) {
            return value == null || value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
        }
    }
}
