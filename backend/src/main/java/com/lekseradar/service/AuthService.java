package com.lekseradar.service;

import com.lekseradar.dto.auth.AuthResponseDTO;
import com.lekseradar.dto.auth.LoginRequestDTO;
import com.lekseradar.dto.auth.RegisterRequestDTO;
import com.lekseradar.dto.user.UserResponseDTO;
import com.lekseradar.exception.ConflictException;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.exception.UnauthorizedException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.UserEventRepository;
import com.lekseradar.repository.UserRepository;
import com.lekseradar.security.JwtService;
import com.lekseradar.security.RateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;

@Service
public class AuthService {
    /** Перебор пароля: после 5 неверных входов на один email за 15 минут вход на него закрыт до конца окна. */
    static final int MAX_FAILED_LOGINS = 5;
    static final Duration FAILED_LOGIN_WINDOW = Duration.ofMinutes(15);

    private final UserRepository users;
    private final UserEventRepository events;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RateLimiter limiter;
    /** Хеш для проверки, когда email не найден: ответ занимает столько же времени и не выдаёт, есть ли такой ученик. */
    private final String dummyHash;

    public AuthService(UserRepository users, UserEventRepository events, PasswordEncoder encoder, JwtService jwt,
                       RateLimiter limiter) {
        this.users = users;
        this.events = events;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limiter = limiter;
        this.dummyHash = encoder.encode("timing-equalizer-not-a-password");
    }

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO req) {
        String email = normalizeEmail(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        UserEntity user = new UserEntity(email, encoder.encode(req.password()), req.displayName().trim());
        user.markLoggedIn();
        users.save(user);
        events.add(user.getId(), "login", null, null);
        return toResponse(user);
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO req) {
        String email = normalizeEmail(req.email());
        String key = "login-failed:" + email;
        if (limiter.count(key, FAILED_LOGIN_WINDOW) >= MAX_FAILED_LOGINS) {
            throw new TooManyRequestsException("Слишком много неверных попыток входа. Попробуйте через 15 минут");
        }
        UserEntity user = users.findByEmailIgnoreCase(email).orElse(null);
        boolean ok = encoder.matches(req.password(), user == null ? dummyHash : user.getPasswordHash());
        if (!ok) user = null;
        if (user == null) {
            limiter.record(key);
            throw new UnauthorizedException("Неверный email или пароль");
        }
        limiter.reset(key);
        user.markLoggedIn();
        events.add(user.getId(), "login", null, null);
        return toResponse(user);
    }

    private AuthResponseDTO toResponse(UserEntity user) {
        return new AuthResponseDTO(jwt.issue(user.getId()), UserResponseDTO.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
