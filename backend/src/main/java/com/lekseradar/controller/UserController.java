package com.lekseradar.controller;

import com.lekseradar.dto.reminder.ReminderTestResponseDTO;
import com.lekseradar.dto.user.ChangePasswordRequestDTO;
import com.lekseradar.dto.user.UpdateProfileRequestDTO;
import com.lekseradar.dto.user.UserResponseDTO;
import com.lekseradar.model.UserEntity;
import com.lekseradar.security.CurrentUser;
import com.lekseradar.service.ReminderService;
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
    private final ReminderService reminders;

    public UserController(UserService users, ReminderService reminders) {
        this.users = users;
        this.reminders = reminders;
    }

    @GetMapping
    @Operation(summary = "Мой профиль")
    public UserResponseDTO me(@CurrentUser UserEntity user) {
        return UserResponseDTO.from(user);
    }

    @PatchMapping
    @Operation(summary = "Изменить имя, язык, часовой пояс или напоминания на почту")
    public UserResponseDTO updateProfile(@CurrentUser UserEntity user, @Valid @RequestBody UpdateProfileRequestDTO req) {
        return users.updateProfile(user.getId(), req);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Сменить пароль", description = "Нужен текущий пароль. Выданные токены продолжают работать")
    public void changePassword(@CurrentUser UserEntity user, @Valid @RequestBody ChangePasswordRequestDTO req) {
        users.changePassword(user.getId(), req);
    }

    @PostMapping("/reminders/test")
    @Operation(summary = "Пробное напоминание на почту",
            description = "Письмо на почту из профиля: несданные задания со сроком в ближайшие 7 дней и просроченные. "
                    + "503 — почта на сервере не настроена")
    public ReminderTestResponseDTO testReminder(@CurrentUser UserEntity user) {
        return reminders.sendTest(user.getId());
    }
}
