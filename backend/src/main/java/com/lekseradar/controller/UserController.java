package com.lekseradar.controller;

import com.lekseradar.dto.user.ChangePasswordRequest;
import com.lekseradar.dto.user.UpdateProfileRequest;
import com.lekseradar.dto.user.UserResponse;
import com.lekseradar.model.UserEntity;
import com.lekseradar.security.CurrentUser;
import com.lekseradar.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "2. Профиль", description = "Текущий пользователь")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "Мой профиль")
    public UserResponse me(@CurrentUser UserEntity user) {
        return UserResponse.from(user);
    }

    @PatchMapping
    @Operation(summary = "Изменить имя, язык или часовой пояс")
    public UserResponse updateProfile(@CurrentUser UserEntity user, @Valid @RequestBody UpdateProfileRequest req) {
        return users.updateProfile(user.getId(), req);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Сменить пароль", description = "Нужен текущий пароль. Выданные токены продолжают работать")
    public void changePassword(@CurrentUser UserEntity user, @Valid @RequestBody ChangePasswordRequest req) {
        users.changePassword(user.getId(), req);
    }
}
