package com.lekseradar.controller;

import com.lekseradar.dto.auth.AuthResponse;
import com.lekseradar.dto.auth.LoginRequest;
import com.lekseradar.dto.auth.RegisterRequest;
import com.lekseradar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Авторизация", description = "Регистрация и вход, токен не нужен")
@SecurityRequirements
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация", description = "Возвращает JWT-токен и профиль")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход", description = "Возвращает JWT-токен и профиль")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }
}
