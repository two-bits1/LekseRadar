package com.lekseradar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import com.lekseradar.dto.user.DeleteAccountRequestDTO;
import com.lekseradar.service.AccountService;
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
    private final AccountService account;

    public UserController(UserService users, ReminderService reminders, AccountService account) {
        this.users = users;
        this.reminders = reminders;
        this.account = account;
    }

    @GetMapping("/export")
    @Operation(summary = "Скачать мои данные (GDPR, ст. 15 и 20)",
            description = "Все данные ученика одним JSON-файлом: профиль, платформы (без доступов), задания, чаты, "
                    + "квизы и ответы, карточки, история, напоминания. Не чаще 5 раз в час")
    public ResponseEntity<String> export(@CurrentUser UserEntity user) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lekseradar-data.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(account.export(user.getId()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить аккаунт и все данные (GDPR, ст. 17)",
            description = "Нужен текущий пароль. Удаляется всё: профиль, платформы (доступ Google отзывается), задания, "
                    + "чаты, квизы, карточки, история. Отменить нельзя")
    public void delete(@CurrentUser UserEntity user, @Valid @RequestBody DeleteAccountRequestDTO req) {
        account.delete(user.getId(), req.password());
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
