package com.lekseradar.service;

import com.lekseradar.dto.user.ChangePasswordRequestDTO;
import com.lekseradar.dto.user.UpdateProfileRequestDTO;
import com.lekseradar.dto.user.UserResponseDTO;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public UserResponseDTO updateProfile(Long userId, UpdateProfileRequestDTO req) {
        UserEntity user = load(userId);
        if (req.displayName() != null && !req.displayName().isBlank()) {
            user.setDisplayName(req.displayName().trim());
        }
        if (req.locale() != null) {
            user.setLocale(req.locale());
        }
        if (req.timezone() != null) {
            user.setTimezone(validZone(req.timezone().trim()));
        }
        if (req.emailReminders() != null) {
            user.setEmailReminders(req.emailReminders());
        }
        return UserResponseDTO.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDTO req) {
        UserEntity user = load(userId);
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Текущий пароль указан неверно");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
    }

    private UserEntity load(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    /** Часовой пояс используется в видах прогресса (AT TIME ZONE), поэтому проверяем его заранее. */
    private static String validZone(String tz) {
        try {
            return ZoneId.of(tz).getId();
        } catch (DateTimeException e) {
            throw new BadRequestException("Неизвестный часовой пояс: " + tz);
        }
    }
}
