package com.lekseradar.controller;

import com.lekseradar.dto.auth.AuthResponseDTO;
import com.lekseradar.dto.auth.LoginRequestDTO;
import com.lekseradar.dto.auth.RegisterRequestDTO;
import com.lekseradar.dto.auth.RegisterStartedResponseDTO;
import com.lekseradar.dto.auth.VerifyEmailRequestDTO;
import com.lekseradar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Авторизация", description = "Регистрация с кодом на email и вход, токен не нужен")
@SecurityRequirements
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Регистрация, шаг 1: код на email",
            description = "Аккаунт ещё не создаётся: на email уходит 6-значный код (15 минут). Повторный вызов — новый код")
    public RegisterStartedResponseDTO register(@Valid @RequestBody RegisterRequestDTO req, HttpServletRequest http) {
        return auth.register(req, http.getRemoteAddr());
    }

    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация, шаг 2: код из письма", description = "Создаёт аккаунт, возвращает JWT-токен и профиль")
    public AuthResponseDTO verify(@Valid @RequestBody VerifyEmailRequestDTO req) {
        return auth.verify(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход", description = "Только для подтверждённых email. Возвращает JWT-токен и профиль")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO req) {
        return auth.login(req);
    }
}
