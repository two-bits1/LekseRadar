package com.lekseradar.service;

import com.lekseradar.config.AppConfig;
import com.lekseradar.dto.auth.LoginRequestDTO;
import com.lekseradar.dto.auth.RegisterRequestDTO;
import com.lekseradar.dto.auth.VerifyEmailRequestDTO;
import com.lekseradar.exception.ApiException;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.exception.ConflictException;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.exception.UnauthorizedException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.UserEventRepository;
import com.lekseradar.repository.UserRepository;
import com.lekseradar.security.JwtService;
import com.lekseradar.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private static final String EMAIL = "ola@example.com";

    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-07T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private UserRepository users;
    private MailService mail;
    private TestClock clock;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        mail = mock(MailService.class);
        clock = new TestClock();
        JwtService jwt = new JwtService(new AppConfig(new AppConfig.Jwt("unit-test-secret-unit-test-secret-1234", 1),
                null, null, null, null, null, null, null));
        auth = new AuthService(users, mock(UserEventRepository.class), new BCryptPasswordEncoder(4), jwt,
                new RateLimiter(), mail, clock);
        when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);
    }

    private String register(String ip) {
        auth.register(new RegisterRequestDTO(" Ola@Example.com ", "password123", " Ola "), ip);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mail, atLeastOnce()).sendVerificationCode(eq(EMAIL), eq("Ola"), code.capture(), eq(15L));
        return code.getValue();
    }

    @Test
    void registerSendsCodeAndCreatesNoUser() {
        String code = register("1.1.1.1");
        assertTrue(code.matches("\\d{6}"));
        verify(users, never()).save(any());
    }

    @Test
    void rightCodeCreatesUserAndLogsIn() {
        String code = register("1.1.1.1");
        var res = auth.verify(new VerifyEmailRequestDTO(EMAIL, " " + code + " "));
        assertNotNull(res.token());
        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).save(saved.capture());
        assertEquals(EMAIL, saved.getValue().getEmail());
        assertEquals("Ola", saved.getValue().getDisplayName());
        assertTrue(new BCryptPasswordEncoder().matches("password123", saved.getValue().getPasswordHash()));
        // код одноразовый
        assertThrows(BadRequestException.class, () -> auth.verify(new VerifyEmailRequestDTO(EMAIL, code)));
    }

    @Test
    void wrongCodeFiveTimesKillsRegistration() {
        String code = register("1.1.1.1");
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < AuthService.MAX_CODE_ATTEMPTS; i++) {
            assertThrows(BadRequestException.class, () -> auth.verify(new VerifyEmailRequestDTO(EMAIL, wrong)));
        }
        assertThrows(BadRequestException.class, () -> auth.verify(new VerifyEmailRequestDTO(EMAIL, code)));
        verify(users, never()).save(any());
    }

    @Test
    void codeExpiresAfter15Minutes() {
        String code = register("1.1.1.1");
        clock.now = clock.now.plus(Duration.ofMinutes(16));
        assertThrows(BadRequestException.class, () -> auth.verify(new VerifyEmailRequestDTO(EMAIL, code)));
    }

    @Test
    void newCodeReplacesOld() {
        String first = register("1.1.1.1");
        String second = register("1.1.1.1");
        if (!first.equals(second)) {
            assertThrows(BadRequestException.class, () -> auth.verify(new VerifyEmailRequestDTO(EMAIL, first)));
        }
        assertNotNull(auth.verify(new VerifyEmailRequestDTO(EMAIL, second)).token());
    }

    @Test
    void existingEmailIs409AndNoMail() {
        when(users.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> auth.register(new RegisterRequestDTO(EMAIL, "password123", "Ola"), "1.1.1.1"));
        verifyNoInteractions(mail);
    }

    @Test
    void mailLimitPerEmail() {
        for (int i = 0; i < AuthService.MAX_CODES_PER_EMAIL; i++) register("ip-" + i);
        assertThrows(TooManyRequestsException.class,
                () -> auth.register(new RegisterRequestDTO(EMAIL, "password123", "Ola"), "ip-x"));
    }

    @Test
    void mailFailureLeavesNoPendingRegistration() {
        doThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Не удалось отправить письмо"))
                .when(mail).sendVerificationCode(anyString(), anyString(), anyString(), anyLong());
        assertThrows(ApiException.class,
                () -> auth.register(new RegisterRequestDTO(EMAIL, "password123", "Ola"), "1.1.1.1"));
        assertThrows(BadRequestException.class, () -> auth.verify(new VerifyEmailRequestDTO(EMAIL, "123456")));
    }

    @Test
    void loginBeforeVerifyExplainsWhy() {
        register("1.1.1.1");
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        UnauthorizedException e = assertThrows(UnauthorizedException.class,
                () -> auth.login(new LoginRequestDTO(EMAIL, "password123")));
        assertTrue(e.getMessage().contains("не подтверждена"));
    }

    @Test
    void loginWithRealPasswordWorksAfterVerify() {
        String code = register("1.1.1.1");
        auth.verify(new VerifyEmailRequestDTO(EMAIL, code));
        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(users).save(saved.capture());
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(saved.getValue()));

        assertNotNull(auth.login(new LoginRequestDTO(EMAIL, "password123")).token());
        assertThrows(UnauthorizedException.class, () -> auth.login(new LoginRequestDTO(EMAIL, "wrong-password")));
    }
}
