package com.lekseradar.service;

import com.lekseradar.dto.auth.AuthResponse;
import com.lekseradar.dto.auth.LoginRequest;
import com.lekseradar.dto.auth.RegisterRequest;
import com.lekseradar.dto.user.UserResponse;
import com.lekseradar.exception.ConflictException;
import com.lekseradar.exception.UnauthorizedException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.UserEventRepository;
import com.lekseradar.repository.UserRepository;
import com.lekseradar.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserEventRepository events;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, UserEventRepository events, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.events = events;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
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
    public AuthResponse login(LoginRequest req) {
        UserEntity user = users.findByEmailIgnoreCase(normalizeEmail(req.email()))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Неверный email или пароль"));
        user.markLoggedIn();
        events.add(user.getId(), "login", null, null);
        return toResponse(user);
    }

    private AuthResponse toResponse(UserEntity user) {
        return new AuthResponse(jwt.issue(user.getId()), UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
