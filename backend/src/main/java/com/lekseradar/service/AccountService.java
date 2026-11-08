package com.lekseradar.service;

import com.lekseradar.exception.BadRequestException;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.model.PlatformConnectionEntity;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.PlatformConnectionRepository;
import com.lekseradar.repository.UserRepository;
import com.lekseradar.security.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Права ученика по GDPR: выгрузка всех данных (ст. 15, 20) и удаление аккаунта (ст. 17).
 * Удаление: сначала каждая платформа отключается (задания и всё сделанное по ним удаляются, доступ Google
 * отзывается), потом строка users — остальное база удаляет каскадом (ON DELETE CASCADE во всех таблицах ученика).
 */
@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    static final int EXPORTS_PER_HOUR = 5;

    private final UserRepository users;
    private final PlatformConnectionRepository connections;
    private final PlatformService platforms;
    private final PasswordEncoder encoder;
    private final RateLimiter limiter;

    public AccountService(UserRepository users, PlatformConnectionRepository connections, PlatformService platforms,
                          PasswordEncoder encoder, RateLimiter limiter) {
        this.users = users;
        this.connections = connections;
        this.platforms = platforms;
        this.encoder = encoder;
        this.limiter = limiter;
    }

    @Transactional(readOnly = true)
    public String export(Long userId) {
        if (!limiter.tryAcquire("export:" + userId, EXPORTS_PER_HOUR, Duration.ofHours(1))) {
            throw new TooManyRequestsException("Выгрузка данных — не чаще 5 раз в час", 3600L);
        }
        String json = users.exportJson(userId);
        if (json == null) throw new NotFoundException("Пользователь не найден");
        return json;
    }

    /** Ученик сам удаляет аккаунт: нужен текущий пароль. */
    public void delete(Long userId, String password) {
        UserEntity user = users.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new BadRequestException("Пароль указан неверно");
        }
        deleteAll(userId);
    }

    /** Удалить аккаунт и все данные (и для срока хранения, без пароля). */
    public void deleteAll(Long userId) {
        for (PlatformConnectionEntity c : connections.findByUserIdOrderByPlatform(userId)) {
            platforms.disconnect(userId, c.getPlatform());
        }
        users.deleteAccount(userId);
        log.info("account {} deleted with all data", userId);
    }
}
